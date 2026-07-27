#include <cstdio>
#include <cstdint>
#include <cstring>
#include <vector>
#include <chrono>
#include <random>

using namespace std;
using Clock = chrono::steady_clock;

// ---------------------------------------------------------------------------
//  Static geometry
// ---------------------------------------------------------------------------
static int yx_to_cell[6][6];
static int cell_to_x[32];
static int cell_to_y[32];
static int LINE[200][4];        
static int CELL_LINES[32][30];  
static int CELL_NLINES[32];    
static int num_lines = 0;

static void initGeometry() {
    int c = 0;
    for (int y = 0; y < 6; y++) {
        for (int x = 0; x < 6; x++) {
            if ((x==0 && y==0) || (x==0 && y==5) || (x==5 && y==0) || (x==5 && y==5)) {
                yx_to_cell[y][x] = -1;
            } else {
                yx_to_cell[y][x] = c;
                cell_to_y[c] = y;
                cell_to_x[c] = x;
                c++;
            }
        }
    }
    
    for (int y = 0; y < 6; y++) {
        for (int x = 0; x <= 2; x++) {
            if (yx_to_cell[y][x] >= 0 && yx_to_cell[y][x+1] >= 0 && yx_to_cell[y][x+2] >= 0 && yx_to_cell[y][x+3] >= 0) {
                LINE[num_lines][0] = yx_to_cell[y][x]; LINE[num_lines][1] = yx_to_cell[y][x+1];
                LINE[num_lines][2] = yx_to_cell[y][x+2]; LINE[num_lines][3] = yx_to_cell[y][x+3];
                num_lines++;
            }
        }
    }
    for (int x = 0; x < 6; x++) {
        for (int y = 0; y <= 2; y++) {
            if (yx_to_cell[y][x] >= 0 && yx_to_cell[y+1][x] >= 0 && yx_to_cell[y+2][x] >= 0 && yx_to_cell[y+3][x] >= 0) {
                LINE[num_lines][0] = yx_to_cell[y][x]; LINE[num_lines][1] = yx_to_cell[y+1][x];
                LINE[num_lines][2] = yx_to_cell[y+2][x]; LINE[num_lines][3] = yx_to_cell[y+3][x];
                num_lines++;
            }
        }
    }
    for (int y = 0; y <= 2; y++) {
        for (int x = 0; x <= 2; x++) {
            if (yx_to_cell[y][x] >= 0 && yx_to_cell[y+1][x+1] >= 0 && yx_to_cell[y+2][x+2] >= 0 && yx_to_cell[y+3][x+3] >= 0) {
                LINE[num_lines][0] = yx_to_cell[y][x]; LINE[num_lines][1] = yx_to_cell[y+1][x+1];
                LINE[num_lines][2] = yx_to_cell[y+2][x+2]; LINE[num_lines][3] = yx_to_cell[y+3][x+3];
                num_lines++;
            }
        }
    }
    for (int y = 0; y <= 2; y++) {
        for (int x = 3; x <= 5; x++) {
            if (yx_to_cell[y][x] >= 0 && yx_to_cell[y+1][x-1] >= 0 && yx_to_cell[y+2][x-2] >= 0 && yx_to_cell[y+3][x-3] >= 0) {
                LINE[num_lines][0] = yx_to_cell[y][x]; LINE[num_lines][1] = yx_to_cell[y+1][x-1];
                LINE[num_lines][2] = yx_to_cell[y+2][x-2]; LINE[num_lines][3] = yx_to_cell[y+3][x-3];
                num_lines++;
            }
        }
    }
    for (int y = 0; y <= 4; y++) {
        for (int x = 0; x <= 4; x++) {
            if (yx_to_cell[y][x] >= 0 && yx_to_cell[y][x+1] >= 0 && yx_to_cell[y+1][x] >= 0 && yx_to_cell[y+1][x+1] >= 0) {
                LINE[num_lines][0] = yx_to_cell[y][x]; LINE[num_lines][1] = yx_to_cell[y][x+1];
                LINE[num_lines][2] = yx_to_cell[y+1][x]; LINE[num_lines][3] = yx_to_cell[y+1][x+1];
                num_lines++;
            }
        }
    }

    for (int c = 0; c < 32; c++) CELL_NLINES[c] = 0;
    for (int li = 0; li < num_lines; li++)
        for (int k = 0; k < 4; k++) {
            int cell_idx = LINE[li][k];
            CELL_LINES[cell_idx][CELL_NLINES[cell_idx]++] = li;
        }
}

// 5 attributes -> 0x1F mask
static inline bool shareChar(int a, int b, int c, int d) {
    int And = a & b & c & d;
    int Or  = a | b | c | d;
    return (And | ((~Or) & 0x1F)) != 0;
}

static int8_t cell[32];   
static uint32_t occupied;   
static uint32_t availMask;  

static uint64_t Z[32][32]; 
static uint64_t ZH[32];    
static uint64_t hashKey;   

static void initZobrist() {
    mt19937_64 rng(0x9E3779B97F4A7C15ULL);
    for (int c = 0; c < 32; c++)
        for (int p = 0; p < 32; p++) Z[c][p] = rng();
    for (int p = 0; p < 32; p++) ZH[p] = rng();
}

static const int TT_BITS = 22;                 // ~100 MB
static const int TT_SIZE = 1 << TT_BITS;
static const int TT_MASK = TT_SIZE - 1;
struct TTEntry {
    uint64_t key;
    int16_t  value;
    int16_t  depth;   
    int8_t   flag;    
    int8_t   cellBest;
    int8_t   give;    
};
static vector<TTEntry> TT;

static const int WIN = 30000;
static const int INF = 1 << 30;

static Clock::time_point deadline;
static bool     aborted;
static uint64_t nodeCount;

static inline bool timeUp() {
    if ((nodeCount & 255) == 0)
        if (Clock::now() >= deadline) return true;
    return false;
}

static inline bool placeWins(int c, int p) {
    for (int i = 0; i < CELL_NLINES[c]; i++) {
        const int* L = LINE[CELL_LINES[c][i]];
        int a = (L[0]==c) ? p : cell[L[0]];
        int b = (L[1]==c) ? p : cell[L[1]];
        int d = (L[2]==c) ? p : cell[L[2]];
        int e = (L[3]==c) ? p : cell[L[3]];
        if (a>=0 && b>=0 && d>=0 && e>=0 && shareChar(a,b,d,e)) return true;
    }
    return false;
}

static inline bool giveIsDangerous(int g) {
    uint32_t em = ~occupied;
    while (em) {
        int c = __builtin_ctz(em);
        em &= em - 1;
        if (placeWins(c, g)) return true;
    }
    return false;
}

static int heurEval() {
    int safe = 0, danger = 0;
    uint32_t am = availMask;
    while (am) {
        int g = __builtin_ctz(am);
        am &= am - 1;
        if (giveIsDangerous(g)) danger++; else safe++;
    }
    if (safe == 0 && danger > 0) return -WIN / 2; 
    return safe * 4 - danger; 
}

static int negamax(int hand, int alpha, int beta, int ply, int maxply) {
    if (aborted || timeUp()) { aborted = true; return 0; }
    nodeCount++;

    uint32_t em = ~occupied;
    {
        uint32_t e = em;
        while (e) {
            int c = __builtin_ctz(e);
            e &= e - 1;
            if (placeWins(c, hand)) return WIN - ply;
        }
    }

    if (availMask == 0) return 0; 
    if (ply >= maxply) return heurEval();

    int alphaOrig = alpha;
    uint64_t key = hashKey;
    TTEntry& te = TT[key & TT_MASK];
    int ttCell = -1, ttGive = -1;
    int remaining = maxply - ply;
    if (te.key == key) {
        ttCell = te.cellBest; ttGive = te.give;
        if (te.depth >= remaining) {
            if (te.flag == 0) return te.value;
            if (te.flag == 1 && te.value > alpha) alpha = te.value;
            else if (te.flag == 2 && te.value < beta) beta = te.value;
            if (alpha >= beta) return te.value;
        }
    }

    int best = -INF, bestCell = -1, bestGive = -1;

    int safeGives[32], nSafe = 0, dangerGives[32], nDanger = 0;
    {
        uint32_t am = availMask;
        while (am) {
            int g = __builtin_ctz(am);
            am &= am - 1;
            if (giveIsDangerous(g)) dangerGives[nDanger++] = g;
            else                    safeGives[nSafe++]     = g;
        }
    }

    int cellsOrder[32], nCells = 0;
    if (ttCell >= 0 && ((occupied >> ttCell) & 1) == 0) cellsOrder[nCells++] = ttCell;
    {
        uint32_t e = em;
        while (e) {
            int c = __builtin_ctz(e);
            e &= e - 1;
            if (c != ttCell) cellsOrder[nCells++] = c;
        }
    }

    for (int ci = 0; ci < nCells && best < beta; ci++) {
        int c = cellsOrder[ci];
        cell[c] = (int8_t)hand;
        occupied |= (1U << c);
        hashKey ^= Z[c][hand];

        for (int phase = 0; phase < 3 && best < beta; phase++) {
            int cnt = (phase == 0) ? (ttGive >= 0 ? 1 : 0)
                    : (phase == 1) ? nSafe : nDanger;
            for (int gi = 0; gi < cnt; gi++) {
                int g = (phase == 0) ? ttGive : (phase == 1 ? safeGives[gi] : dangerGives[gi]);
                if (((availMask >> g) & 1) == 0) continue; 
                if (phase != 0 && g == ttGive) continue;   

                availMask &= ~(1U << g);
                hashKey ^= ZH[g];

                int val = -negamax(g, -beta, -alpha, ply + 1, maxply);

                hashKey ^= ZH[g];
                availMask |= (1U << g);

                if (aborted) { 
                    hashKey ^= Z[c][hand];
                    occupied &= ~(1U << c);
                    cell[c] = -1;
                    return 0;
                }

                if (val > best) { best = val; bestCell = c; bestGive = g; }
                if (best > alpha) alpha = best;
                if (alpha >= beta) break; 
            }
        }

        hashKey ^= Z[c][hand];
        occupied &= ~(1U << c);
        cell[c] = -1;
    }

    te.key = key;
    te.value = (int16_t)best;
    te.depth = (int16_t)remaining;
    te.cellBest = (int8_t)bestCell;
    te.give = (int8_t)bestGive;
    te.flag = (best <= alphaOrig) ? 2 : (best >= beta ? 1 : 0);
    return best;
}

struct RootMove { int cell; int give; int score; };

static RootMove rootSearchPlace(int hand, int maxply, RootMove prev) {
    RootMove bestMove = prev;
    int alpha = -INF, beta = INF, best = -INF;

    uint32_t em = ~occupied;

    int cellsOrder[32], nCells = 0;
    if (prev.cell >= 0 && ((occupied >> prev.cell) & 1) == 0) cellsOrder[nCells++] = prev.cell;
    { uint32_t e = em; while (e) { int c = __builtin_ctz(e); e &= e-1; if (c != prev.cell) cellsOrder[nCells++] = c; } }

    for (int ci = 0; ci < nCells; ci++) {
        int c = cellsOrder[ci];
        cell[c] = (int8_t)hand;
        occupied |= (1U << c);
        hashKey ^= Z[c][hand];

        int order[32], no = 0;
        bool prevGiveUsed = false;
        if (ci == 0 && prev.give >= 0 && ((availMask >> prev.give) & 1)) { order[no++] = prev.give; prevGiveUsed = true; }
        { uint32_t am = availMask; while (am) { int g = __builtin_ctz(am); am &= am-1;
              if (prevGiveUsed && g == prev.give) continue;
              if (!giveIsDangerous(g)) order[no++] = g; } }
        { uint32_t am = availMask; while (am) { int g = __builtin_ctz(am); am &= am-1;
              if (prevGiveUsed && g == prev.give) continue;
              if (giveIsDangerous(g)) order[no++] = g; } }

        for (int gi = 0; gi < no; gi++) {
            int g = order[gi];
            availMask &= ~(1U << g);
            hashKey ^= ZH[g];
            int val = -negamax(g, -beta, -alpha, 1, maxply);
            hashKey ^= ZH[g];
            availMask |= (1U << g);

            if (aborted) { hashKey ^= Z[c][hand]; occupied &= ~(1U<<c); cell[c] = -1; return prev; }

            if (val > best) { best = val; bestMove.cell = c; bestMove.give = g; bestMove.score = val; }
            if (best > alpha) alpha = best;
        }

        hashKey ^= Z[c][hand];
        occupied &= ~(1U << c);
        cell[c] = -1;
    }
    return bestMove;
}

static int rootChooseFirst(int maxply, int prevGive) {
    int alpha = -INF, beta = INF, best = -INF, bestGive = prevGive;
    int order[32], no = 0;
    if (prevGive >= 0 && ((availMask >> prevGive) & 1)) order[no++] = prevGive;
    { uint32_t am = availMask; while (am) { int g = __builtin_ctz(am); am &= am-1; if (g != prevGive) order[no++] = g; } }

    for (int gi = 0; gi < no; gi++) {
        int g = order[gi];
        availMask &= ~(1U << g);
        hashKey ^= ZH[g];
        int val = -negamax(g, -beta, -alpha, 1, maxply);
        hashKey ^= ZH[g];
        availMask |= (1U << g);
        if (aborted) return bestGive;
        if (val > best) { best = val; bestGive = g; }
        if (best > alpha) alpha = best;
    }
    return bestGive;
}

int main() {
    initGeometry();
    initZobrist();
    TT.assign(TT_SIZE, TTEntry{0,0,0,0,-1,-1});

    int pieceToPlace;
    bool firstTurn = true;

    int myId;
    if (scanf("%d", &myId) != 1) return 0;

    while (scanf("%d", &pieceToPlace) == 1) {
        occupied = 0;
        hashKey = 0;
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                int v; if (scanf("%d", &v) != 1) return 0;
                int k = yx_to_cell[y][x];
                if (k >= 0) {
                    cell[k] = (int8_t)v;
                    if (v >= 0) { occupied |= (1U << k); hashKey ^= Z[k][v]; }
                }
            }
        }
        int n; if (scanf("%d", &n) != 1) return 0;
        availMask = 0;
        for (int i = 0; i < n; i++) {
            int v; if (scanf("%d", &v) != 1) return 0;
            availMask |= (1U << v);
        }
        if (pieceToPlace >= 0) hashKey ^= ZH[pieceToPlace];

        int budgetMs = firstTurn ? 450 : 38;
        deadline = Clock::now() + chrono::milliseconds(budgetMs);
        aborted = false;
        nodeCount = 0;

        if (pieceToPlace == -1) {
            int give = -1;
            int prev = __builtin_ctz(availMask); 
            for (int d = 2; d <= 6; d++) { // shallow search
                int g = rootChooseFirst(d, prev);
                if (aborted) break;
                prev = g; give = g;
            }
            if (give < 0) give = __builtin_ctz(availMask);
            printf("CHOOSE %d\n", give);
            fflush(stdout);
            firstTurn = false;
            continue;
        }

        if (n == 0) {
            int c = __builtin_ctz(~occupied);
            printf("%d %d\n", cell_to_x[c], cell_to_y[c]);
            fflush(stdout);
            firstTurn = false;
            continue;
        }

        int winCell = -1;
        {
            uint32_t e = ~occupied;
            while (e) { int c = __builtin_ctz(e); e &= e-1;
                if (placeWins(c, pieceToPlace)) { winCell = c; break; } }
        }
        if (winCell >= 0) {
            int give = -1;
            cell[winCell] = (int8_t)pieceToPlace; occupied |= (1U<<winCell);
            uint32_t a2 = availMask;
            while (a2) { int g = __builtin_ctz(a2); a2 &= a2-1;
                if (!giveIsDangerous(g)) { give = g; break; } }
            occupied &= ~(1U<<winCell); cell[winCell] = -1;
            if (give < 0) give = __builtin_ctz(availMask);
            printf("%d %d %d\n", cell_to_x[winCell], cell_to_y[winCell], give);
            fflush(stdout);
            firstTurn = false;
            continue;
        }

        RootMove best{ -1, -1, -INF };
        {
            int c = __builtin_ctz(~occupied);
            best.cell = c;
            int give = -1;
            cell[c] = (int8_t)pieceToPlace; occupied |= (1U<<c);
            uint32_t a2 = availMask; while (a2) { int g = __builtin_ctz(a2); a2 &= a2-1; if (!giveIsDangerous(g)) { give = g; break; } }
            occupied &= ~(1U<<c); cell[c] = -1;
            best.give = (give >= 0) ? give : __builtin_ctz(availMask);
        }

        for (int d = 1; d <= 6; d++) {
            RootMove r = rootSearchPlace(pieceToPlace, d, best);
            if (aborted) break;
            best = r;
            if (best.score >= WIN - 100) break; 
        }

        printf("%d %d %d\n", cell_to_x[best.cell], cell_to_y[best.cell], best.give);
        fflush(stdout);
        firstTurn = false;
    }
    return 0;
}
