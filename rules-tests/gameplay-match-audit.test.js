// rules-tests/gameplay-match-audit.test.js
const assert = require('assert');

const BASE_URL = 'http://127.0.0.1:9000';
const AUTH_URL = 'http://127.0.0.1:9099';
const NS = 'scribble-9bba6-default-rtdb';

async function createUser(prefix) {
  const email = `${prefix}_${Math.random().toString(36).substring(2, 9)}@example.com`;
  const res = await fetch(`${AUTH_URL}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: 'password123', returnSecureToken: true })
  });
  const data = await res.json();
  return { uid: data.localId, token: data.idToken };
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const queryParams = [`ns=${NS}`];
  if (options.admin) {
    headers['Authorization'] = 'Bearer owner';
  } else if (options.token) {
    queryParams.push(`auth=${options.token}`);
  }
  const normalizedPath = path.startsWith('/') ? path : (path ? '/' + path : '');
  const url = `${BASE_URL}${normalizedPath || '/'}.json?${queryParams.join('&')}`;
  const res = await fetch(url, {
    method: options.method || 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch (e) { json = text; }
  return { status: res.status, ok: res.ok, data: json };
}

async function auditMatchFlow() {
  console.log('====================================================');
  console.log('GAMEPLAY COMPATIBILITY AUDIT: ACTUAL CODE REPO FLOWS');
  console.log('====================================================\n');

  // Clear DB
  await request('', { method: 'DELETE', admin: true });

  const host = await createUser('host');
  const guesser = await createUser('guesser');
  const roomId = 'room_match_audit';
  const turnId = 'turn_1';

  // STEP 1: Room Creation (FirebaseRoomRepository.createRoom)
  // Host calls roomsRef.child(assignedRoomId).setValue(roomDto)
  console.log('Step 1: createRoom() by Host...');
  const createRes = await request(`/rooms/${roomId}`, {
    method: 'PUT',
    token: host.token,
    body: {
      id: roomId,
      code: 'AUD101',
      hostId: host.uid,
      status: 'WAITING',
      createdAt: Date.now(),
      lastActivityAt: Date.now(),
      settings: { maxPlayers: 8, rounds: 3, drawTimeSeconds: 60, wordDifficulty: 'MEDIUM' },
      players: {
        [host.uid]: {
          id: host.uid,
          name: 'HostPlayer',
          avatarId: 'CAT',
          isHost: true,
          score: 0,
          joinedAt: Date.now(),
          isConnected: true
        }
      }
    }
  });
  console.log(`  -> createRoom result: status ${createRes.status}`);

  // STEP 2: Join Room (FirebaseRoomRepository.joinRoom -> executeJoinTransaction)
  // Code runs: roomRef.runTransaction(...) on /rooms/$roomId!
  console.log('\nStep 2: joinRoom() via executeJoinTransaction (roomRef.runTransaction on /rooms/$roomId)...');
  const joinTxRes = await request(`/rooms/${roomId}`, {
    method: 'PATCH', // Transaction writes updated room object or patch to /rooms/$roomId
    token: guesser.token,
    body: {
      [`players/${guesser.uid}`]: {
        id: guesser.uid,
        name: 'GuesserPlayer',
        avatarId: 'DOG',
        isHost: false,
        score: 0,
        joinedAt: Date.now(),
        isConnected: true
      },
      lastActivityAt: Date.now()
    }
  });
  console.log(`  -> executeJoinTransaction on /rooms/$roomId result: status ${joinTxRes.status}`);

  // STEP 3: Start Game (FirebaseRoomRepository.startGame)
  // Code runs: roomRef.runTransaction(...) on /rooms/$roomId updating status to IN_PROGRESS!
  console.log('\nStep 3: startGame() via roomRef.runTransaction on /rooms/$roomId...');
  const startTxRes = await request(`/rooms/${roomId}`, {
    method: 'PATCH',
    token: host.token,
    body: {
      status: 'IN_PROGRESS',
      lastActivityAt: Date.now()
    }
  });
  console.log(`  -> startGame transaction on /rooms/$roomId result: status ${startTxRes.status}`);

  // STEP 4: Initialize Game (FirebaseGameRepository.initializeGame)
  // 4A: Host writes secret word choices for first drawer: secretsRef.child(roomId).child(firstDrawerId).setValue(secretDto)
  console.log('\nStep 4A: initializeGame() host writes secret word choices for first drawer (guesser)...');
  const initSecretRes = await request(`/gameSecrets/${roomId}/${guesser.uid}`, {
    method: 'PUT',
    token: host.token, // HOST writes secret for GUESSER
    body: {
      turnId: turnId,
      drawerId: guesser.uid,
      selectedWord: null,
      wordChoices: [{ id: '1', word: 'APPLE', difficulty: 'EASY' }]
    }
  });
  console.log(`  -> host writing secret for drawer result: status ${initSecretRes.status}`);

  // 4B: Host updates gameState and status: roomsRef.child(roomId).updateChildren(...)
  console.log('\nStep 4B: initializeGame() host updates gameState on /rooms/$roomId...');
  const initGameRes = await request(`/rooms/${roomId}/gameState`, {
    method: 'PUT',
    token: host.token,
    body: {
      phase: 'SELECTING_WORD',
      round: 1,
      totalRounds: 3,
      turnNumber: 1,
      turnId: turnId,
      drawerId: host.uid,
      turnDeadline: Date.now() + 20000,
      maskedWord: '',
      wordLength: 0,
      wordHash: '',
      revealedWord: null,
      correctGuessers: null,
      turnOrder: [host.uid, guesser.uid],
      clearSeq: 0
    }
  });
  console.log(`  -> host initializing gameState result: status ${initGameRes.status}`);

  // STEP 5: Word Selection (FirebaseGameRepository.selectWord)
  // Drawer writes to /rooms/$roomId/gameState.updateChildren(updates with correctGuessers: null)
  console.log('\nStep 5: selectWord() drawer updates gameState with correctGuessers=null...');
  const selectWordRes = await request(`/rooms/${roomId}/gameState`, {
    method: 'PATCH',
    token: host.token,
    body: {
      phase: 'DRAWING',
      maskedWord: '_ _ _ _ _',
      wordLength: 5,
      wordHash: 'hash_apple',
      turnDeadline: Date.now() + 60000,
      revealedWord: null,
      correctGuessers: null
    }
  });
  console.log(`  -> selectWord result: status ${selectWordRes.status}`);

  // STEP 6: Submit Guess (FirebaseGameRepository.submitGuess)
  // Guesser runs transaction on roomRef (/rooms/$roomId) updating player score, correctGuessers, and chat!
  console.log('\nStep 6: submitGuess() guesser updates score, correctGuessers and chat via transaction on /rooms/$roomId...');
  const guessScoreRes = await request(`/rooms/${roomId}/players/${guesser.uid}/score`, {
    method: 'PUT',
    token: guesser.token,
    body: 100
  });
  console.log(`  -> guesser updating score result: status ${guessScoreRes.status}`);

  const guessSystemChatRes = await request(`/rooms/${roomId}/chat/msg_sys_guess`, {
    method: 'PUT',
    token: guesser.token,
    body: {
      id: 'msg_sys_guess',
      senderId: guesser.uid,
      senderName: 'GuesserPlayer',
      text: 'guessed the word!',
      type: 'CORRECT_GUESS',
      timestamp: Date.now()
    }
  });
  console.log(`  -> guesser posting CORRECT_GUESS chat result: status ${guessSystemChatRes.status}`);

  // STEP 7: Clear Canvas (FirebaseGameRepository.clearDrawing)
  // Drawer calls turnDrawingRef.updateChildren({ strokes: null, clearSeq: ... })
  console.log('\nStep 7: clearDrawing() drawer sets strokes=null and clearSeq=TIMESTAMP...');
  const clearStrokesRes = await request(`/rooms/${roomId}/drawing/${turnId}`, {
    method: 'PATCH',
    token: host.token,
    body: {
      strokes: null,
      clearSeq: Date.now()
    }
  });
  console.log(`  -> clearDrawing result: status ${clearStrokesRes.status}`);

  // STEP 8: End Turn & Advance (FirebaseGameRepository.endTurnAndAdvance)
  // Client updates drawer score, round, turnNumber, turnId, drawerId, and writes next secrets!
  console.log('\nStep 8: endTurnAndAdvance() client updates round, turnNumber, drawerId...');
  const advanceTurnRes = await request(`/rooms/${roomId}/gameState`, {
    method: 'PATCH',
    token: host.token,
    body: {
      phase: 'SELECTING_WORD',
      round: 1,
      turnNumber: 2,
      turnId: 'turn_2',
      drawerId: guesser.uid,
      turnDeadline: Date.now() + 20000
    }
  });
  console.log(`  -> endTurnAndAdvance gameState update result: status ${advanceTurnRes.status}`);

  console.log('\n====================================================');
  console.log('AUDIT RUN COMPLETE');
  console.log('====================================================\n');
}

auditMatchFlow().catch(console.error);
