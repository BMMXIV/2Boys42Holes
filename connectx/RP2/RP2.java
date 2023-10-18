package connectx.RP2;

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
public class RP2 implements CXPlayer {
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

    public class Score {
        public int val;
        public int column;

        public Score() {
            val = -M*N;
            column = -1;
        }
    }

	/* Default empty constructor */
	public RP2() {
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
			evaluateColumns(B, L);
			return score.column;
		} catch (TimeoutException e) {
			System.err.println("Timeout!!! Random column selected");
			return score.column;
		}
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

    private Score evaluateColumns(CXBoard B, Integer[] L) throws TimeoutException {
        Score eval = new Score();

		if (B.gameState() == draw) {
            eval.val = 0;
            return eval;
        }

        eval = singleMoveWin(B, L);
        if (eval.column != -1) {
            return eval;
        }

        for(int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            eval = evaluateColumns(B, B.getAvailableColumns());
            if (eval.val > score.val) {
                score.val = eval.val;
                score.column = i;
            }
            B.unmarkColumn();
        }
        return score;
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
			if (state == myWin) {
                eval.val = (M*N + 1 - B.numOfMarkedCells());
                eval.column = i;
                B.unmarkColumn();
                return eval;
            } else if (state == yourWin) {
                eval.val = (-M*N + 1 + B.numOfMarkedCells());
                eval.column = i;
                B.unmarkColumn();
                return eval;
            }
            B.unmarkColumn();
		}
		return eval;
	}

	public String playerName() {
		return "Rdy Player One";
	}
}
