import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { setGlobalOptions } from "firebase-functions/v2";

admin.initializeApp();
setGlobalOptions({ region: "us-central1" });

/**
 * Authoritative replay voting callable.
 * Accepts: { roomId: string, vote: boolean }
 */
export const submitPlayAgainVote = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const { roomId, vote } = request.data || {};
  if (!roomId || typeof roomId !== "string" || typeof vote !== "boolean") {
    throw new HttpsError("invalid-argument", "Valid roomId and boolean vote are required.");
  }

  const db = admin.database();
  const roomRef = db.ref(`rooms/${roomId}`);

  let restartTriggered = false;
  let sessionClosed = false;
  let currentVotesCount = 0;
  let requiredVotesCount = 0;

  const txResult = await roomRef.transaction((room) => {
    if (!room) {
      return room; // Room does not exist
    }

    // Verify caller is a recognized participant in this room
    if (!room.players || !room.players[uid]) {
      return; // Abort transaction - nonparticipant
    }

    // If session is already closed, prevent votes
    if (room.status === "CLOSED" || (room.replayVoting && room.replayVoting.status === "CLOSED")) {
      sessionClosed = true;
      return room;
    }

    const now = Date.now();
    const voting = room.replayVoting || {};
    const deadline = voting.deadline || (now + 15000);

    // If voting deadline has expired without majority, mark CLOSED
    if (now > deadline && voting.status !== "RESTARTED") {
      sessionClosed = true;
      room.status = "CLOSED";
      room.closeReason = "REPLAY_VOTE_FAILED";
      room.closedAt = now;
      room.replayVoting = {
        ...voting,
        status: "CLOSED",
        failed: true,
        deadline: deadline,
      };
      return room;
    }

    // If already restarted, do not accept further replay vote changes
    if (voting.status === "RESTARTED" || room.status === "IN_PROGRESS") {
      restartTriggered = true;
      return room;
    }

    // Enforce 1 vote per authenticated player
    const votes = { ...(voting.votes || {}) };
    votes[uid] = vote;

    // Calculate eligible participants & strict majority floor(N / 2) + 1
    const playerIds = Object.keys(room.players);
    const totalParticipants = playerIds.length;
    const requiredMajority = Math.floor(totalParticipants / 2) + 1;

    let yesCount = 0;
    for (const pid of playerIds) {
      if (votes[pid] === true) {
        yesCount++;
      }
    }

    currentVotesCount = yesCount;
    requiredVotesCount = requiredMajority;

    if (yesCount >= requiredMajority) {
      // STRICT MAJORITY REACHED -> RESTART MATCH
      restartTriggered = true;
      room.status = "IN_PROGRESS";
      room.matchSeq = (room.matchSeq || 1) + 1;
      room.replayVoting = {
        status: "RESTARTED",
        votes: votes,
        voteCount: yesCount,
        requiredMajority: requiredMajority,
        deadline: deadline,
      };

      const turnOrder = [...playerIds].sort();
      const firstDrawerId = turnOrder[0];
      const countdownDeadline = now + 5000;

      const initialScores: Record<string, number> = {};
      for (const pid of playerIds) {
        initialScores[pid] = 0;
        if (room.players[pid]) {
          room.players[pid].score = 0;
        }
      }

      const totalRounds = (room.settings && room.settings.rounds) ? room.settings.rounds : 3;

      room.gameState = {
        phase: "COUNTDOWN",
        round: 1,
        totalRounds: totalRounds,
        turnNumber: 1,
        turnId: "turn_1",
        drawerId: firstDrawerId,
        turnDeadline: countdownDeadline,
        maskedWord: "",
        wordLength: 0,
        wordHash: "",
        revealedWord: null,
        correctGuessers: null,
        turnOrder: turnOrder,
        clearSeq: 0,
        scores: initialScores,
      };

      // Wipe previous drawing
      delete room.drawing;
    } else {
      // Updated votes, still in VOTING phase
      room.replayVoting = {
        status: "VOTING",
        votes: votes,
        voteCount: yesCount,
        requiredMajority: requiredMajority,
        deadline: deadline,
      };
    }

    return room;
  });

  if (!txResult.committed) {
    throw new HttpsError("permission-denied", "Only room participants can vote on replay.");
  }

  const updatedRoom = txResult.snapshot.val();
  if (!updatedRoom) {
    throw new HttpsError("not-found", "Room not found.");
  }

  if (sessionClosed) {
    if (updatedRoom.code) {
      try {
        const codeSnap = await db.ref(`roomCodes/${updatedRoom.code}`).once("value");
        if (codeSnap.val() === roomId) {
          await db.ref(`roomCodes/${updatedRoom.code}`).remove();
        }
      } catch (err) {
        console.warn(`Could not remove roomCode ${updatedRoom.code}:`, err);
      }
    }
    return {
      status: "CLOSED",
      restarted: false,
      message: "Replay voting expired. Session is closed.",
    };
  }

  if (restartTriggered) {
    // Clean up old secrets for this room
    await db.ref(`gameSecrets/${roomId}`).remove().catch(() => null);
    return {
      status: "RESTARTED",
      restarted: true,
      voteCount: currentVotesCount,
      requiredMajority: requiredVotesCount,
    };
  }

  return {
    status: "VOTING",
    restarted: false,
    voteCount: currentVotesCount,
    requiredMajority: requiredVotesCount,
  };
});

/**
 * Authoritative session finalization callable.
 * Called when a participant's timer expires or when leaving.
 */
export const finalizeReplaySession = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const { roomId } = request.data || {};
  if (!roomId || typeof roomId !== "string") {
    throw new HttpsError("invalid-argument", "Valid roomId is required.");
  }

  const db = admin.database();
  const roomSnap = await db.ref(`rooms/${roomId}`).once("value");
  if (!roomSnap.exists()) {
    return { status: "CLOSED", alreadyDeleted: true };
  }

  const room = roomSnap.val();
  // Check if caller is/was participant
  if (!room.players || !room.players[uid]) {
    throw new HttpsError("permission-denied", "Only participants can request finalization.");
  }

  // If match has already restarted, do not finalize
  if (room.status === "IN_PROGRESS" || (room.replayVoting && room.replayVoting.status === "RESTARTED")) {
    return { status: "RESTARTED", finalized: false };
  }

  // Record match results if finished and not yet recorded
  if (room.status === "FINISHED" || (room.gameState && room.gameState.phase === "MATCH_RESULTS")) {
    const matchSeq = room.matchSeq || 1;
    await recordAuthoritativeMatchResults(room, roomId, `${roomId}_m${matchSeq}`).catch((err) => {
      console.warn("Could not record match results on finalizeReplaySession:", err);
    });
  }

  // Remove roomCode mapping immediately so code cannot be reused for stale room
  if (room.code) {
    try {
      const codeSnap = await db.ref(`roomCodes/${room.code}`).once("value");
      if (codeSnap.val() === roomId) {
        await db.ref(`roomCodes/${room.code}`).remove();
      }
    } catch (err) {
      console.warn(`Could not remove roomCode ${room.code}:`, err);
    }
  }

  // Authoritatively set room status and replay voting status to CLOSED with reason
  const now = Date.now();
  await db.ref(`rooms/${roomId}/status`).set("CLOSED");
  await db.ref(`rooms/${roomId}/closeReason`).set("REPLAY_VOTE_FAILED");
  await db.ref(`rooms/${roomId}/closedAt`).set(now);
  await db.ref(`rooms/${roomId}/replayVoting/status`).set("CLOSED");
  await db.ref(`rooms/${roomId}/replayVoting/failed`).set(true);
  await db.ref(`gameSecrets/${roomId}`).remove().catch(() => null);

  return { status: "CLOSED", finalized: true };
});

/**
 * Scheduled cleanup: runs every 1 minute to sweep expired replay sessions
 * and abandoned finished rooms where all clients disconnected.
 */
export const checkReplayTimeouts = onSchedule(
  {
    schedule: "every 1 minutes",
    region: "us-central1",
    retryCount: 1,
  },
  async () => {
    const db = admin.database();
    const roomsSnap = await db.ref("rooms").once("value");
    if (!roomsSnap.exists()) return;

    const now = Date.now();
    const promises: Promise<void>[] = [];

    roomsSnap.forEach((child) => {
      const room = child.val();
      const roomId = child.key;
      if (!roomId || !room) return;

      const isFinished = room.status === "FINISHED";
      const isClosed = room.status === "CLOSED";
      const voting = room.replayVoting || {};

      // 1. Voting deadline expired without restart
      const deadlineExpired = voting.deadline && now >= voting.deadline && voting.status !== "RESTARTED";

      // 2. Room was closed for at least 20 seconds (allowing all clients to receive CLOSED event and navigate)
      const closedExpired = isClosed && (room.closedAt ? (now - room.closedAt > 20000) : true);

      // 3. Abandoned finished room without active replay activity for > 2 minutes
      const finishedAbandoned = isFinished && room.lastActivityAt && (now - room.lastActivityAt > 120000);

      if (closedExpired || (isFinished && deadlineExpired) || finishedAbandoned) {
        promises.push(
          (async () => {
            try {
              await db.ref(`rooms/${roomId}/status`).set("CLOSED");
              await cleanupRoomAndCode(db, roomId, room.code);
            } catch (err) {
              console.error(`Error cleaning up expired room ${roomId}:`, err);
            }
          })()
        );
      }
    });

    await Promise.all(promises);
  }
);

/**
 * Helper to remove room and only delete the room-code if it points to this roomId.
 */
async function cleanupRoomAndCode(db: admin.database.Database, roomId: string, roomCode?: string) {
  if (roomCode) {
    try {
      const codeSnap = await db.ref(`roomCodes/${roomCode}`).once("value");
      if (codeSnap.val() === roomId) {
        await db.ref(`roomCodes/${roomCode}`).remove();
      }
    } catch (err) {
      console.warn(`Could not remove roomCode ${roomCode}:`, err);
    }
  }

  await db.ref(`gameSecrets/${roomId}`).remove().catch(() => null);
  await db.ref(`rooms/${roomId}`).remove().catch(() => null);
}

/**
 * Authoritative match finalization callable.
 * Accepts: { roomId: string }
 * Derives final scores, placements, and lifetime statistics from trusted RTDB gameState.
 */
export const finalizeMatchStats = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const { roomId } = request.data || {};
  if (!roomId || typeof roomId !== "string") {
    throw new HttpsError("invalid-argument", "Valid roomId is required.");
  }

  const db = admin.database();
  const roomSnap = await db.ref(`rooms/${roomId}`).once("value");
  if (!roomSnap.exists()) {
    throw new HttpsError("not-found", "Room not found.");
  }

  const room = roomSnap.val();
  if (!room.players || !room.players[uid]) {
    throw new HttpsError("permission-denied", "Only participants can finalize match stats.");
  }

  const phase = room.gameState?.phase;
  const status = room.status;
  if (phase !== "MATCH_RESULTS" && status !== "FINISHED") {
    return { status: "MATCH_NOT_FINISHED", finalized: false };
  }

  const matchSeq = room.matchSeq || 1;
  const matchId = `${roomId}_m${matchSeq}`;

  const result = await recordAuthoritativeMatchResults(room, roomId, matchId);
  return { status: "SUCCESS", ...result };
});

/**
 * Helper to record match results into Firestore and update player lifetime statistics atomically.
 */
async function recordAuthoritativeMatchResults(
  room: any,
  roomId: string,
  matchId: string
): Promise<{ finalized: boolean; matchId: string; alreadyFinalized?: boolean; reason?: string }> {
  const firestore = admin.firestore();
  const matchDocRef = firestore.collection("matches").doc(matchId);

  // Fast read check
  const existingSnap = await matchDocRef.get();
  if (existingSnap.exists) {
    return { finalized: true, matchId, alreadyFinalized: true };
  }

  // Derive scores from trusted RTDB gameState.scores (fallback to players scores)
  const gameState = room.gameState || {};
  const rawScores: Record<string, number> = gameState.scores || {};
  const players = room.players || {};
  const playerUids = Object.keys(players);

  // Need at least 2 participants for a valid multiplayer match
  if (playerUids.length < 2) {
    return { finalized: false, matchId, reason: "NOT_ENOUGH_PLAYERS" };
  }

  // Build final scores map for all participants
  const finalScores: Record<string, number> = {};
  for (const pid of playerUids) {
    finalScores[pid] = typeof rawScores[pid] === "number" ? rawScores[pid] : (players[pid]?.score || 0);
  }

  // Calculate highest score and determine winner / draw
  let highestScore = -1;
  for (const pid of playerUids) {
    const score = finalScores[pid];
    if (score > highestScore) {
      highestScore = score;
    }
  }

  // Check if match was abandoned immediately without playing
  if (highestScore <= 0 && (gameState.round || 1) <= 1 && (gameState.turnNumber || 1) <= 1) {
    return { finalized: false, matchId, reason: "ABANDONED_BEFORE_PLAY" };
  }

  const topScorers = playerUids.filter((pid) => finalScores[pid] === highestScore);
  const isDraw = topScorers.length > 1;
  const winnerUid = isDraw ? null : topScorers[0];

  const totalRounds = gameState.totalRounds || room.settings?.rounds || 3;
  const drawTimeSeconds = room.settings?.drawTimeSeconds || 60;

  // Execute Firestore transaction for exact-once execution
  return await firestore.runTransaction(async (transaction) => {
    const freshMatchDoc = await transaction.get(matchDocRef);
    if (freshMatchDoc.exists) {
      return { finalized: true, matchId, alreadyFinalized: true };
    }

    // Read all participant user docs
    const userDocRefs = playerUids.map((pid) => firestore.collection("users").doc(pid));
    const userDocs = await Promise.all(userDocRefs.map((ref) => transaction.get(ref)));

    const now = Date.now();

    // Prepare match document
    transaction.set(matchDocRef, {
      matchId: matchId,
      roomId: roomId,
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
      playerUids: playerUids,
      scores: finalScores,
      winnerUid: winnerUid,
      isDraw: isDraw,
      rounds: totalRounds,
      drawTimeSeconds: drawTimeSeconds,
    });

    // Update each participant's stats or guest counter
    userDocs.forEach((uDoc, index) => {
      const pid = playerUids[index];
      const pScore = finalScores[pid] || 0;
      const isWinner = !isDraw && winnerUid === pid;
      const isTied = isDraw && topScorers.includes(pid);

      let accountType = "PERMANENT";
      let guestMatchesCompleted = 0;

      let existingStats = {
        gamesPlayed: 0,
        wins: 0,
        losses: 0,
        draws: 0,
        totalScore: 0,
        bestScore: 0,
        winStreak: 0,
      };

      if (uDoc.exists) {
        const uData = uDoc.data() || {};
        accountType = uData.accountType || "PERMANENT";
        guestMatchesCompleted = typeof uData.guestMatchesCompleted === "number" ? uData.guestMatchesCompleted : 0;

        const s = uData.stats || {};
        existingStats = {
          gamesPlayed: typeof s.gamesPlayed === "number" ? s.gamesPlayed : 0,
          wins: typeof s.wins === "number" ? s.wins : 0,
          losses: typeof s.losses === "number" ? s.losses : 0,
          draws: typeof s.draws === "number" ? s.draws : 0,
          totalScore: typeof s.totalScore === "number" ? s.totalScore : 0,
          bestScore: typeof s.bestScore === "number" ? s.bestScore : 0,
          winStreak: typeof s.winStreak === "number" ? s.winStreak : 0,
        };
      }

      if (accountType === "GUEST") {
        const updatedGuestMatches = guestMatchesCompleted + 1;
        const updatedStatus = updatedGuestMatches >= 2 ? "EXHAUSTED" : "ACTIVE";

        if (uDoc.exists) {
          transaction.update(userDocRefs[index], {
            guestMatchesCompleted: updatedGuestMatches,
            guestAccessStatus: updatedStatus,
            updatedAt: now,
            lastSeenAt: now,
          });
        } else {
          const playerName = players[pid]?.name || `Guest_${pid.slice(0, 4)}`;
          const playerAvatar = players[pid]?.avatarId || "cat";
          transaction.set(userDocRefs[index], {
            uid: pid,
            displayName: playerName,
            avatarId: playerAvatar,
            accountType: "GUEST",
            guestMatchesCompleted: updatedGuestMatches,
            guestAccessStatus: updatedStatus,
            createdAt: now,
            updatedAt: now,
            lastSeenAt: now,
            preferences: { soundEnabled: true },
          });
        }
      } else {
        const updatedGamesPlayed = existingStats.gamesPlayed + 1;
        const updatedWins = existingStats.wins + (isWinner ? 1 : 0);
        const updatedDraws = existingStats.draws + (isTied ? 1 : 0);
        const updatedLosses = existingStats.losses + (!isWinner && !isTied ? 1 : 0);
        const updatedTotalScore = existingStats.totalScore + pScore;
        const updatedBestScore = Math.max(existingStats.bestScore, pScore);
        const updatedWinStreak = isWinner ? existingStats.winStreak + 1 : 0;

        const updatedStats = {
          gamesPlayed: updatedGamesPlayed,
          wins: updatedWins,
          losses: updatedLosses,
          draws: updatedDraws,
          totalScore: updatedTotalScore,
          bestScore: updatedBestScore,
          winStreak: updatedWinStreak,
        };

        if (uDoc.exists) {
          transaction.update(userDocRefs[index], {
            stats: updatedStats,
            updatedAt: now,
            lastSeenAt: now,
          });
        } else {
          const playerName = players[pid]?.name || `Player_${pid.slice(0, 4)}`;
          const playerAvatar = players[pid]?.avatarId || "cat";
          transaction.set(userDocRefs[index], {
            uid: pid,
            displayName: playerName,
            avatarId: playerAvatar,
            accountType: "PERMANENT",
            guestMatchesCompleted: 0,
            guestAccessStatus: "ACTIVE",
            createdAt: now,
            updatedAt: now,
            lastSeenAt: now,
            stats: updatedStats,
            preferences: { soundEnabled: true },
          });
        }
      }
    });

    return { finalized: true, matchId };
  });
}

/**
 * Authoritative check for guest match eligibility.
 * Ensures caller can participate in or create a match.
 */
export const checkGuestEligibility = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const firestore = admin.firestore();
  const userDoc = await firestore.collection("users").doc(uid).get();

  if (!userDoc.exists) {
    return { eligible: true, accountType: "GUEST", remainingMatches: 2 };
  }

  const data = userDoc.data() || {};
  const accountType = data.accountType || "PERMANENT";

  if (accountType === "PERMANENT") {
    return { eligible: true, accountType: "PERMANENT", remainingMatches: -1 };
  }

  const matchesCompleted = typeof data.guestMatchesCompleted === "number" ? data.guestMatchesCompleted : 0;
  const accessStatus = data.guestAccessStatus || "ACTIVE";
  const eligible = matchesCompleted < 2 && accessStatus !== "EXHAUSTED";
  const remaining = Math.max(0, 2 - matchesCompleted);

  return {
    eligible: eligible,
    accountType: "GUEST",
    matchesCompleted: matchesCompleted,
    remainingMatches: remaining,
    guestAccessStatus: accessStatus,
  };
});

/**
 * Upgrades a guest user to a permanent account upon linking with Google.
 */
export const upgradeGuestAccount = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const firestore = admin.firestore();
  const userRef = firestore.collection("users").doc(uid);
  const now = Date.now();

  await firestore.runTransaction(async (transaction) => {
    const doc = await transaction.get(userRef);
    if (!doc.exists) {
      return;
    }
    transaction.update(userRef, {
      accountType: "PERMANENT",
      guestAccessStatus: "ACTIVE",
      updatedAt: now,
    });
  });

  return { success: true, uid };
});

/**
 * Scheduled cleanup: runs daily to sweep abandoned guest accounts older than 30 days without activity.
 * Verifies:
 * - account is GUEST
 * - not currently in an active game
 * - preserves match records in /matches
 * - deletes user doc in Firestore and deletes auth user
 */
export const cleanupAbandonedGuestAccounts = onSchedule(
  {
    schedule: "every 24 hours",
    region: "us-central1",
    retryCount: 1,
  },
  async () => {
    const firestore = admin.firestore();
    const db = admin.database();
    const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);

    // Query guest users whose lastSeenAt is older than 30 days
    const snapshot = await firestore
      .collection("users")
      .where("accountType", "==", "GUEST")
      .where("lastSeenAt", "<=", thirtyDaysAgo)
      .limit(100)
      .get();

    if (snapshot.empty) {
      console.log("No abandoned guest accounts found to clean up.");
      return;
    }

    // Fetch active room player IDs to make sure none of these guests are currently in an active game
    const activeRoomsSnap = await db.ref("rooms").once("value");
    const activePlayerUids = new Set<string>();
    if (activeRoomsSnap.exists()) {
      activeRoomsSnap.forEach((roomSnap) => {
        const room = roomSnap.val();
        if (room && (room.status === "WAITING" || room.status === "IN_PROGRESS") && room.players) {
          Object.keys(room.players).forEach((pid) => activePlayerUids.add(pid));
        }
      });
    }

    const batch = firestore.batch();
    const deletedUids: string[] = [];

    for (const doc of snapshot.docs) {
      const uid = doc.id;
      if (activePlayerUids.has(uid)) {
        continue; // Skip user currently in an active game
      }

      batch.delete(doc.ref);
      deletedUids.push(uid);
    }

    if (deletedUids.length > 0) {
      await batch.commit();
      for (const uid of deletedUids) {
        try {
          await admin.auth().deleteUser(uid);
        } catch (err) {
          console.warn(`Could not delete Auth user ${uid}:`, err);
        }
      }
      console.log(`Cleaned up ${deletedUids.length} abandoned guest accounts.`);
    }
  }
);
