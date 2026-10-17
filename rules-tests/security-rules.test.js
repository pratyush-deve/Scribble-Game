// rules-tests/security-rules.test.js
const assert = require('assert');

const BASE_URL = 'http://127.0.0.1:9000';
const AUTH_URL = 'http://127.0.0.1:9099';
const NS = 'scribble-9bba6-default-rtdb';

async function createUser(prefix) {
  const email = `${prefix}_${Math.random().toString(36).substring(2, 9)}@example.com`;
  const res = await fetch(`${AUTH_URL}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password: 'password123',
      returnSecureToken: true
    })
  });
  const data = await res.json();
  if (!data.idToken) {
    throw new Error(`Failed to create user ${prefix}: ${JSON.stringify(data)}`);
  }
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

async function runTests() {
  console.log('====================================================');
  console.log('RUNNING EXPANDED FIREBASE RTDB SECURITY RULES TESTS');
  console.log('====================================================\n');

  let passed = 0;
  let failed = 0;

  async function test(name, fn) {
    try {
      await fn();
      console.log(`  [PASS] ${name}`);
      passed++;
    } catch (err) {
      console.error(`  [FAIL] ${name}: ${err.message}`);
      failed++;
    }
  }

  // Clear database before tests
  await request('', { method: 'DELETE', admin: true });

  // Create test actors
  const host = await createUser('host');
  const drawer = await createUser('drawer');
  const guesser = await createUser('guesser');
  const attacker = await createUser('attacker');
  const roomId = 'room_sec_test';
  const turnId = 'turn_1';

  console.log(`Actors initialized:`);
  console.log(`  Host:     ${host.uid}`);
  console.log(`  Drawer:   ${drawer.uid}`);
  console.log(`  Guesser:  ${guesser.uid}`);
  console.log(`  Attacker: ${attacker.uid}\n`);

  // --- SECTION 1: ROOM LIFECYCLE & PARENT-LEVEL LOCKDOWN ---
  await test('Host creates room with initial settings and host player record', async () => {
    const res = await request(`/rooms/${roomId}`, {
      method: 'PUT',
      token: host.token,
      body: {
        id: roomId,
        code: 'SEC101',
        hostId: host.uid,
        status: 'WAITING',
        createdAt: Date.now(),
        settings: {
          maxPlayers: 8,
          rounds: 3,
          drawTimeSeconds: 60,
          wordDifficulty: 'MEDIUM'
        },
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
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Room creation by unauthenticated client is denied', async () => {
    const res = await request('/rooms/unauth_room', {
      method: 'PUT',
      body: { id: 'unauth_room', hostId: 'anyone' }
    });
    assert.strictEqual(res.status, 401);
  });

  await test('Room creation with spoofed hostId is denied', async () => {
    const res = await request('/rooms/spoofed_room', {
      method: 'PUT',
      token: attacker.token,
      body: { id: 'spoofed_room', hostId: host.uid }
    });
    assert.strictEqual(res.status, 401);
  });

  await test('Malicious client attempting room-root overwrite on existing room is denied', async () => {
    const res = await request(`/rooms/${roomId}`, {
      method: 'PUT',
      token: attacker.token,
      body: { id: roomId, hostId: attacker.uid, status: 'CLOSED' }
    });
    assert.strictEqual(res.status, 401, 'Parent-level overwrite must be rejected');
  });

  await test('Member client attempting room-root multi-path patch is denied', async () => {
    const res = await request(`/rooms/${roomId}`, {
      method: 'PATCH',
      token: guesser.token,
      body: { status: 'CLOSED' }
    });
    assert.strictEqual(res.status, 401, 'Parent-level patch must be rejected');
  });

  await test('Non-host attempting to delete room is denied', async () => {
    const res = await request(`/rooms/${roomId}`, {
      method: 'DELETE',
      token: guesser.token
    });
    assert.strictEqual(res.status, 401, 'Only host can delete room');
  });

  // --- SECTION 2: PLAYER RECORD PROTECTION & FIELD SEPARATION ---
  await test('Drawer joins room by writing legitimate initial player record', async () => {
    const res = await request(`/rooms/${roomId}/players/${drawer.uid}`, {
      method: 'PUT',
      token: drawer.token,
      body: {
        id: drawer.uid,
        name: 'DrawerPlayer',
        avatarId: 'DOG',
        isHost: false,
        score: 0,
        joinedAt: Date.now(),
        isConnected: true
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Guesser joins room by writing legitimate initial player record', async () => {
    const res = await request(`/rooms/${roomId}/players/${guesser.uid}`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: guesser.uid,
        name: 'GuesserPlayer',
        avatarId: 'FOX',
        isHost: false,
        score: 0,
        joinedAt: Date.now(),
        isConnected: true
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Joining player cannot grant themselves isHost flag', async () => {
    const res = await request(`/rooms/${roomId}/players/${attacker.uid}`, {
      method: 'PUT',
      token: attacker.token,
      body: {
        id: attacker.uid,
        name: 'Attacker',
        avatarId: 'BEAR',
        isHost: true, // Attempting self-promotion on join
        score: 0,
        joinedAt: Date.now(),
        isConnected: true
      }
    });
    assert.strictEqual(res.status, 401, 'Cannot join with isHost=true unless room host');
  });

  await test('Joining player cannot set initial score > 0', async () => {
    const res = await request(`/rooms/${roomId}/players/${attacker.uid}`, {
      method: 'PUT',
      token: attacker.token,
      body: {
        id: attacker.uid,
        name: 'Attacker',
        avatarId: 'BEAR',
        isHost: false,
        score: 1000, // Cheating score on join
        joinedAt: Date.now(),
        isConnected: true
      }
    });
    assert.strictEqual(res.status, 401, 'Initial score must be 0');
  });

  await test('Guesser attempting to increase their own score directly is denied', async () => {
    const res = await request(`/rooms/${roomId}/players/${guesser.uid}/score`, {
      method: 'PUT',
      token: guesser.token,
      body: 500
    });
    assert.strictEqual(res.status, 401, 'Clients must not be able to write score');
  });

  await test('Player attempting to set their own isHost flag after join is denied', async () => {
    const res = await request(`/rooms/${roomId}/players/${guesser.uid}/isHost`, {
      method: 'PUT',
      token: guesser.token,
      body: true
    });
    assert.strictEqual(res.status, 401, 'Only host can grant isHost');
  });

  await test('Player attempting to modify another player record is denied', async () => {
    const res = await request(`/rooms/${roomId}/players/${host.uid}/name`, {
      method: 'PUT',
      token: guesser.token,
      body: 'GriefedName'
    });
    assert.strictEqual(res.status, 401, 'Cannot edit another player data');
  });

  await test('Player updating their own avatar succeeds', async () => {
    const res = await request(`/rooms/${roomId}/players/${guesser.uid}/avatarId`, {
      method: 'PUT',
      token: guesser.token,
      body: 'PANDA'
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Player updating their own presence (isConnected) succeeds', async () => {
    const res = await request(`/rooms/${roomId}/players/${guesser.uid}/isConnected`, {
      method: 'PUT',
      token: guesser.token,
      body: true
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  // --- SECTION 3: HOST CONTROLS ---
  await test('Non-host attempting to modify settings is denied', async () => {
    const res = await request(`/rooms/${roomId}/settings/rounds`, {
      method: 'PUT',
      token: guesser.token,
      body: 10
    });
    assert.strictEqual(res.status, 401, 'Non-host cannot modify settings');
  });

  await test('Host modifying settings succeeds', async () => {
    const res = await request(`/rooms/${roomId}/settings/rounds`, {
      method: 'PUT',
      token: host.token,
      body: 5
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Non-host attempting to modify room status is denied', async () => {
    const res = await request(`/rooms/${roomId}/status`, {
      method: 'PUT',
      token: guesser.token,
      body: 'IN_PROGRESS'
    });
    assert.strictEqual(res.status, 401, 'Non-host cannot modify status');
  });

  await test('Host updating status to IN_PROGRESS succeeds', async () => {
    const res = await request(`/rooms/${roomId}/status`, {
      method: 'PUT',
      token: host.token,
      body: 'IN_PROGRESS'
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  // --- SECTION 4: GAME STATE LOCKDOWN ---
  await test('Host initializes initial gameState', async () => {
    const res = await request(`/rooms/${roomId}/gameState`, {
      method: 'PUT',
      token: host.token,
      body: {
        phase: 'SELECTING_WORD',
        round: 1,
        totalRounds: 3,
        turnNumber: 1,
        turnId: turnId,
        drawerId: drawer.uid,
        turnDeadline: Date.now() + 20000,
        maskedWord: '',
        wordLength: 0,
        wordHash: '',
        revealedWord: null,
        correctGuessers: null,
        turnOrder: [host.uid, drawer.uid, guesser.uid],
        clearSeq: 0
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Non-host / guesser attempting to overwrite entire gameState is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState`, {
      method: 'PUT',
      token: guesser.token,
      body: { phase: 'FINISHED' }
    });
    assert.strictEqual(res.status, 401, 'Broad gameState overwrite must be denied');
  });

  await test('Guesser attempting to hijack drawerId in gameState is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState/drawerId`, {
      method: 'PUT',
      token: guesser.token,
      body: guesser.uid
    });
    assert.strictEqual(res.status, 401, 'drawerId must not be client-writable');
  });

  await test('Guesser attempting to change turnOrder in gameState is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState/turnOrder`, {
      method: 'PUT',
      token: guesser.token,
      body: [guesser.uid]
    });
    assert.strictEqual(res.status, 401, 'turnOrder must not be client-writable');
  });

  await test('Guesser attempting to declare game FINISHED / winner is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState/phase`, {
      method: 'PUT',
      token: guesser.token,
      body: 'MATCH_RESULTS'
    });
    assert.strictEqual(res.status, 401, 'Cannot arbitrarily change phase or declare winner');
  });

  await test('Drawer selecting word updates maskedWord and transitions phase to DRAWING', async () => {
    const resMask = await request(`/rooms/${roomId}/gameState/maskedWord`, {
      method: 'PUT',
      token: drawer.token,
      body: '_ _ _ _ _'
    });
    assert.strictEqual(resMask.status, 200, `Expected 200, got ${resMask.status}`);

    const resPhase = await request(`/rooms/${roomId}/gameState/phase`, {
      method: 'PUT',
      token: drawer.token,
      body: 'DRAWING'
    });
    assert.strictEqual(resPhase.status, 200, `Expected 200, got ${resPhase.status}`);
  });

  await test('Guesser recording themselves in correctGuessers during DRAWING phase succeeds', async () => {
    const res = await request(`/rooms/${roomId}/gameState/correctGuessers/${guesser.uid}`, {
      method: 'PUT',
      token: guesser.token,
      body: Date.now()
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Guesser attempting to record another player in correctGuessers is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState/correctGuessers/${host.uid}`, {
      method: 'PUT',
      token: guesser.token,
      body: Date.now()
    });
    assert.strictEqual(res.status, 401, 'Guesser cannot record another player');
  });

  await test('Drawer attempting to record themselves in correctGuessers is denied', async () => {
    const res = await request(`/rooms/${roomId}/gameState/correctGuessers/${drawer.uid}`, {
      method: 'PUT',
      token: drawer.token,
      body: Date.now()
    });
    assert.strictEqual(res.status, 401, 'Drawer cannot guess their own drawing');
  });

  // --- SECTION 5: DRAWING STROKES INTEGRITY ---
  await test('Active drawer writing a stroke succeeds', async () => {
    const res = await request(`/rooms/${roomId}/drawing/${turnId}/strokes/s_01`, {
      method: 'PUT',
      token: drawer.token,
      body: {
        id: 's_01',
        turnId: turnId,
        drawerId: drawer.uid,
        colorArgb: -16777216,
        strokeWidth: 5.0,
        points: [0.1, 0.1, 0.2, 0.2],
        deleted: false,
        seq: 1
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Non-drawer attempting to write a stroke is denied', async () => {
    const res = await request(`/rooms/${roomId}/drawing/${turnId}/strokes/s_hack`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: 's_hack',
        turnId: turnId,
        drawerId: guesser.uid,
        colorArgb: -65536,
        strokeWidth: 10.0,
        points: [0.5, 0.5],
        deleted: false,
        seq: 2
      }
    });
    assert.strictEqual(res.status, 401, 'Non-drawer must not write strokes');
  });

  await test('Non-drawer attempting to clear canvas is denied', async () => {
    const res = await request(`/rooms/${roomId}/drawing/${turnId}/clearSeq`, {
      method: 'PUT',
      token: guesser.token,
      body: Date.now()
    });
    assert.strictEqual(res.status, 401, 'Non-drawer must not clear canvas');
  });

  await test('Active drawer clearing canvas succeeds', async () => {
    const res = await request(`/rooms/${roomId}/drawing/${turnId}/clearSeq`, {
      method: 'PUT',
      token: drawer.token,
      body: Date.now()
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  // --- SECTION 6: SECRET WORD ISOLATION ---
  await test('Drawer writing their own secret word succeeds', async () => {
    const res = await request(`/gameSecrets/${roomId}/${drawer.uid}`, {
      method: 'PUT',
      token: drawer.token,
      body: {
        turnId: turnId,
        drawerId: drawer.uid,
        selectedWord: 'ELEPHANT',
        wordChoices: [
          { id: '1', word: 'ELEPHANT', difficulty: 'HARD' }
        ]
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Client attempting to write secret words for another player is denied', async () => {
    const res = await request(`/gameSecrets/${roomId}/${drawer.uid}`, {
      method: 'PUT',
      token: guesser.token, // Guesser attempts to tamper with drawer secret
      body: {
        turnId: turnId,
        drawerId: drawer.uid,
        selectedWord: 'FORGED',
        wordChoices: []
      }
    });
    assert.strictEqual(res.status, 401, 'Cannot write secrets for another player');
  });

  await test('Drawer reading their own secret word choices succeeds', async () => {
    const res = await request(`/gameSecrets/${roomId}/${drawer.uid}`, {
      method: 'GET',
      token: drawer.token
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
    assert.strictEqual(res.data.selectedWord, 'ELEPHANT');
  });

  await test('Non-drawer attempting to read drawer secret word is denied', async () => {
    const res = await request(`/gameSecrets/${roomId}/${drawer.uid}`, {
      method: 'GET',
      token: guesser.token
    });
    assert.strictEqual(res.status, 401, 'Non-drawer must receive 401 on gameSecrets');
  });

  // --- SECTION 7: CHAT INTEGRITY & SYSTEM FORGERY PROTECTION ---
  await test('Player sending chat message with matching senderId succeeds', async () => {
    const res = await request(`/rooms/${roomId}/chat/msg_01`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: 'msg_01',
        senderId: guesser.uid,
        senderName: 'GuesserPlayer',
        text: 'Is it large?',
        type: 'CHAT',
        timestamp: Date.now()
      }
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  await test('Client forging senderId as another player is denied', async () => {
    const res = await request(`/rooms/${roomId}/chat/msg_impersonate`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: 'msg_impersonate',
        senderId: host.uid, // Impersonating host
        senderName: 'HostPlayer',
        text: 'I quit',
        type: 'CHAT',
        timestamp: Date.now()
      }
    });
    assert.strictEqual(res.status, 401, 'Impersonating another senderId must fail');
  });

  await test('Client forging a system chat message (senderId=system) is denied', async () => {
    const res = await request(`/rooms/${roomId}/chat/msg_fake_sys`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: 'msg_fake_sys',
        senderId: 'system', // Forging system
        senderName: 'System',
        text: 'System: Everyone receives 1000 points!',
        type: 'SYSTEM_INFO',
        timestamp: Date.now()
      }
    });
    assert.strictEqual(res.status, 401, 'Client forging system senderId must fail');
  });

  await test('Client forging a SYSTEM_INFO message type is denied', async () => {
    const res = await request(`/rooms/${roomId}/chat/msg_fake_type`, {
      method: 'PUT',
      token: guesser.token,
      body: {
        id: 'msg_fake_type',
        senderId: guesser.uid,
        senderName: 'GuesserPlayer',
        text: 'Fake announcement',
        type: 'SYSTEM_INFO', // Forging SYSTEM_INFO type
        timestamp: Date.now()
      }
    });
    assert.strictEqual(res.status, 401, 'Client forging SYSTEM_INFO type must fail');
  });

  // --- SECTION 8: CLEANUP & ROOM DELETION ---
  await test('Host deleting room succeeds', async () => {
    const res = await request(`/rooms/${roomId}`, {
      method: 'DELETE',
      token: host.token
    });
    assert.strictEqual(res.status, 200, `Expected 200, got ${res.status}`);
  });

  console.log('\n====================================================');
  console.log(`TEST SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('====================================================\n');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests().catch(err => {
  console.error('Test suite failed with unexpected error:', err);
  process.exit(1);
});
