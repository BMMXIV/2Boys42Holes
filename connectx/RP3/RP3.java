package connectx.RP3;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import javax.swing.text.Position;

/**
 * 
 */
public class RP3 implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
    private CXGameState draw;
    //private CXGameState open;
    private Score score = new Score();
	private int  TIMEOUT;
	private long START;
	int M;
	int N;
	int nodes;

    public class Score {
        public int val;
        public int column;
        public int alpha;
        public int beta;

        public Score() {
            val = -M*N;
            column = -1;
            alpha = -((M*N + 1)/2 + 1);
            beta = (M*N + 1)/2 + 1;
        }
    }

	/* Default empty constructor */
	public RP3() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        draw = CXGameState.DRAW;
        //open = CXGameState.OPEN;
		TIMEOUT = timeout_in_secs;
		this.M = M;
		this.N = N;
	}

	/**
	 * Selects a free colum on game board.
	 * <p>
	 * Selects a winning column (if any), otherwise selects column with best score 
	 * If there's a timeout exception, selects a random column.
	 * </p>
	 */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
		score.column = L[rand.nextInt(L.length)]; // Save a random column 

		try {
			//score[0] = column to play, score[1] = evaluation
			score = evaluateColumns(B, L, -M*N, M*N);
			return score.column;
		} catch (TimeoutException e) {
			System.err.println("Timeout");
			return score.column;
		}
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

    private Score evaluateColumns(CXBoard B, Integer[] L, int alpha, int beta) throws TimeoutException {
        Score eval = new Score();
        Score tmp = new Score();
		nodes++;
		//int alpha = -M*N;
		//int beta = M*N;

		if (B.gameState() == draw) {
            eval.val = 0;
			eval.alpha = Math.max(eval.val, eval.alpha);
            return eval;
        }

        eval = singleMoveWin(B, L);
        if (eval.column != -1) {
			int temp = eval.alpha;
			eval.alpha = -eval.beta;
			eval.beta = -temp;
			eval.alpha = Math.max(eval.val, eval.alpha);
            return eval;
        }

		//alpha and beta are the opposite of alpha and beta of the other player
		eval.alpha = -beta;
		eval.beta = -alpha;

        for(int i : L) {
            checktime(); // Check timeout at every iteration
            
			B.markColumn(i);
            tmp = evaluateColumns(B, B.getAvailableColumns(), eval.alpha, eval.beta);

            tmp.val = -tmp.val;
            if (tmp.val > eval.val) {
                eval.val = tmp.val;
                eval.column = i;
            }
			eval.alpha = Math.max(eval.val, eval.alpha);

			if (eval.alpha >= eval.beta){
				B.unmarkColumn();
				break;
			}
            B.unmarkColumn();
        }
        return eval;
    }

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private Score singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        Score eval = new Score();
		for(int i : L){
			checktime(); // Check timeout at every iteration
			CXGameState state = B.markColumn(i);
			if (state == myWin || state == yourWin) {
                eval.val = (M*N + 1 - B.numOfMarkedCells())/2;
				eval.alpha = eval.val;
                eval.column = i;
                B.unmarkColumn();
                return eval;
            }
            B.unmarkColumn();
		}
		return eval;
	}

	public String playerName() {
		return "Visited Nodes: " + Integer.toString(nodes) +" Rdy Player 3";
	}
}
