package connectx.RP1;

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
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class RP1 implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
    private CXGameState draw;
    //private CXGameState open;
	private int  TIMEOUT;
	private long START;
	int M;
	int N;

	/* Default empty constructor */
	public RP1() {
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
	 * Selects a winning column (if any), otherwise selects a column (if any) 
	 * that prevents the adversary to win with his next move. If both previous
	 * cases do not apply, selects a random column.
	 * </p>
	 */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
		int save = L[rand.nextInt(L.length)]; // Save a random column 

		try
		{
			//score[0] = column to play, score[1] = evaluation
			Integer[] score = evaluateColumns(B, L);
			return score[0];
		}
		catch (TimeoutException e)
		{
			System.err.println("Timeout!!! Random column selected");
			return save;
		}
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

    private Integer[] evaluateColumns(CXBoard B, Integer[] L) throws TimeoutException
    {
		int bestScore = -M*N;
		Integer[] newScore;
		Integer[] eval = {-1, bestScore};	//eval[0] = column to play, eval[1] = best score now

		if (B.gameState() == draw)			//game already ended, it's a draw
		{
			eval[1] = 0;
			return eval;
		}
		if (B.gameState() == myWin)			//game already ended, it's a win
		{
			eval[1] = (M*N + 1 - B.numOfMarkedCells())/2;;
			return eval;
		}
		if (B.gameState() == yourWin)		//game already ended, it's a loss
		{
			eval[1] = -(M*N + 1 - B.numOfMarkedCells())/2;;
			return eval;
		}

		//game is still open

		int col = singleMoveWin(B, L);
		if (col != -1)
		{
			eval[0] = col;
			eval[1] = (M*N + 1 - B.numOfMarkedCells())/2;
			return eval;
		}

		/**
		 * If we're here it means there was no win in one move,
		 * otherwise singleMoveWin() would have found it
		 */
		for (int i : L)
		{
			checktime(); // Check timeout at every iteration
			B.markColumn(i);
      		
			newScore = evaluateColumns(B, B.getAvailableColumns());
			newScore[1] = -newScore[1];		//score is the opposite of the score of the other player
			if (newScore[1] > eval[1])
			{
				eval[0] = i;
				eval[1] = newScore[1];
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
	private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
		for(int i : L) {
				checktime(); // Check timeout at every iteration
		  CXGameState state = B.markColumn(i);
		  if (state == myWin)
			return i; // Winning column found: return immediately
		  B.unmarkColumn();
		}
			return -1;
	}

	public String playerName() {
		return "Rdy Player One";
	}
}
