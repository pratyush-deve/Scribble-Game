// rules-tests/gameplay-refactored-flow.test.js
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

async function runRefactoredGameplayFlow() {
  console.log('====================================================');
  console.log('RUNNING REFACTORED GAMEPLAY FLOW TEST');
  console.log('====================================================\n');

  // Clear DB
  await request('', { method: 'DELETE', admin: true });

  const host = await createUser('host');
  const guesser = await createUser('guesser');
  const roomId = 'room_refactored_flow';
  const roomCode = 'FLOW99';
  const turnId = 'turn_1';

  // 1. Host registers roomCode
  const codeRes = await request(`/roomCodes/${roomCode}`, {
    method: 'PUT',
    token: host.token,
    body: roomId
  });
  assert.strictEqual(codeRes.status, 200, 'Host should register roomCode');
  console.log('  [PASS] Host registers roomCode');

  // 2. Host creates room
  const createRoomRes = await request(`/rooms/${roomId}`, {
    method: 'PUT',
    token: host.token,
    body: {
      id: roomId,
      code: roomCode,
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
  assert.strictEqual(createRoomRes.status, 200, 'Host should create room');
  console.log('  [PASS] Host creates room');

  // 3. Guesser joins via narrow path: /rooms/$roomId/players/$guesserId
  const joinRes = await request(`/rooms/${roomId}/players/${guesser.uid}`, {
    method: 'PUT',
    token: guesser.token,
    body: {
      id: guesser.uid,
      name: 'GuesserPlayer',
      avatarId: 'DOG',
      isHost: false,
      score: 0,
      joinedAt: Date.now(),
      isConnected: true
    }
  });
  assert.strictEqual(joinRes.status, 200, 'Guesser should join room at players child path');
  console.log('  [PASS] Guesser joins room via players child path');

  // 4. Host updates settings: /rooms/$roomId/settings
  const updateSettingsRes = await request(`/rooms/${roomId}/settings`, {
    method: 'PUT',
    token: host.token,
    body: {
      maxPlayers: 6,
      rounds: 4,
      drawTimeSeconds: 45,
      wordDifficulty: 'HARD'
    }
  });
  assert.strictEqual(updateSettingsRes.status, 200, 'Host should update settings');
  console.log('  [PASS] Host updates settings');

  // 5. Host starts game: /rooms/$roomId/status = 'IN_PROGRESS'
  const startGameRes = await request(`/rooms/${roomId}/status`, {
    method: 'PUT',
    token: host.token,
    body: 'IN_PROGRESS'
  });
  assert.strictEqual(startGameRes.status, 200, 'Host should set status to IN_PROGRESS');
  console.log('  [PASS] Host starts game (status: IN_PROGRESS)');

  // 6. Host initializes gameState
  const initGameRes = await request(`/rooms/${roomId}/gameState`, {
    method: 'PUT',
    token: host.token,
    body: {
      phase: 'SELECTING_WORD',
      round: 1,
      totalRounds: 4,
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
  assert.strictEqual(initGameRes.status, 200, 'Host should initialize gameState');
  console.log('  [PASS] Host initializes gameState');

  // 7. Active drawer (host) writes secret choices
  const drawerSecretRes = await request(`/gameSecrets/${roomId}/${host.uid}`, {
    method: 'PUT',
    token: host.token,
    body: {
      turnId: turnId,
      drawerId: host.uid,
      selectedWord: null,
      wordChoices: [
        { id: '1', word: 'APPLE', difficulty: 'EASY' },
        { id: '2', word: 'BANANA', difficulty: 'MEDIUM' },
        { id: '3', word: 'CHERRY', difficulty: 'HARD' }
      ]
    }
  });
  assert.strictEqual(drawerSecretRes.status, 200, 'Active drawer should write their secret word choices');
  console.log('  [PASS] Active drawer writes secret word choices');

  // 8. Active drawer selects word (without correctGuessers: null)
  const selectWordRes = await request(`/rooms/${roomId}/gameState`, {
    method: 'PATCH',
    token: host.token,
    body: {
      phase: 'DRAWING',
      maskedWord: '_ _ _ _ _',
      wordLength: 5,
      wordHash: 'apple_hash_123',
      turnDeadline: Date.now() + 60000,
      revealedWord: null
    }
  });
  assert.strictEqual(selectWordRes.status, 200, 'Active drawer should transition to DRAWING phase');
  console.log('  [PASS] Active drawer selects word and enters DRAWING phase');

  // 9. Active drawer sends stroke
  const strokeRes = await request(`/rooms/${roomId}/drawing/${turnId}/strokes/stroke_1`, {
    method: 'PUT',
    token: host.token,
    body: {
      id: 'stroke_1',
      turnId: turnId,
      drawerId: host.uid,
      colorArgb: -16777216,
      strokeWidth: 6.0,
      points: [10.0, 10.0, 20.0, 20.0],
      seq: 1,
      deleted: false
    }
  });
  assert.strictEqual(strokeRes.status, 200, 'Active drawer should send stroke');
  console.log('  [PASS] Active drawer sends drawing stroke');

  // 10. Active drawer clears canvas (marks stroke deleted and updates clearSeq)
  const clearRes = await request(`/rooms/${roomId}/drawing/${turnId}`, {
    method: 'PATCH',
    token: host.token,
    body: {
      'strokes/stroke_1/deleted': true,
      clearSeq: Date.now()
    }
  });
  assert.strictEqual(clearRes.status, 200, 'Active drawer should clear canvas');
  console.log('  [PASS] Active drawer clears canvas');

  // 11. Guesser records correct guess directly at correctGuessers/$guesserId
  const guessRes = await request(`/rooms/${roomId}/gameState/correctGuessers/${guesser.uid}`, {
    method: 'PUT',
    token: guesser.token,
    body: Date.now()
  });
  assert.strictEqual(guessRes.status, 200, 'Guesser should record themselves in correctGuessers');
  console.log('  [PASS] Guesser records correct guess');

  // 12. Guesser posts chat message for correct guess
  const guessChatRes = await request(`/rooms/${roomId}/chat/msg_correct_1`, {
    method: 'PUT',
    token: guesser.token,
    body: {
      id: 'msg_correct_1',
      senderId: guesser.uid,
      senderName: 'GuesserPlayer',
      text: 'guessed the word!',
      type: 'CORRECT_GUESS',
      timestamp: Date.now()
    }
  });
  assert.strictEqual(guessChatRes.status, 200, 'Guesser should post CORRECT_GUESS chat message');
  console.log('  [PASS] Guesser posts CORRECT_GUESS chat message');

  // 13. Guesser leaves room: DELETE /rooms/$roomId/players/$guesserId
  const leaveRes = await request(`/rooms/${roomId}/players/${guesser.uid}`, {
    method: 'DELETE',
    token: guesser.token
  });
  assert.strictEqual(leaveRes.status, 200, 'Guesser should leave room');
  console.log('  [PASS] Guesser leaves room');

  // 14. Host leaves and deletes room + roomCode
  const deleteCodeRes = await request(`/roomCodes/${roomCode}`, {
    method: 'DELETE',
    token: host.token
  });
  assert.strictEqual(deleteCodeRes.status, 200, 'Host should delete roomCode');

  const deleteRoomRes = await request(`/rooms/${roomId}`, {
    method: 'DELETE',
    token: host.token
  });
  assert.strictEqual(deleteRoomRes.status, 200, 'Host should delete room');
  console.log('  [PASS] Host deletes roomCode and room on departure');

  console.log('\n====================================================');
  console.log('REFACTORED GAMEPLAY FLOW TEST: ALL 14 ASSERTIONS PASSED!');
  console.log('====================================================\n');
}

runRefactoredGameplayFlow().catch((err) => {
  console.error('Test failed:', err);
  process.exit(1);
});
