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
 * 
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
	 * Selects a winning column (if any), otherwise selects column with best score 
	 * If there's a timeout exception, selects a random column.
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

		//we know from the recursive call that the game is either drawn or still open
		//and if it's the first call of evaluateColumns and not a recursive one game is open for sure
		
		//we check if it's a draw
		if (B.gameState() == draw)			//game already ended, it's a draw
		{
			eval[1] = 0;
			return eval;
		}
		//game is not drawn, so it must be open

		//game is still open

		//check if there's a move to win immediately
		eval = singleMoveWin(B, L);
		if (eval[0] != -1)
		{
			return eval;
		}

		/**
		 * If we're here it means there was no win in one move,
		 * otherwise singleMoveWin() would have found it
		 */
		for (int i : L)
		{
			checktime(); // Check timeout at every iteration

			//after this move game is either still open or a draw
			//because there was no single move winning
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
	private Integer[] singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
		Integer[] eval = {-1, -M*N};
		for(int i : L)
		{
			checktime(); // Check timeout at every iteration
			CXGameState state = B.markColumn(i);
			if (state == myWin || state == yourWin)
			{
				eval[0] = i;
				eval[1] = (M*N + 1 - B.numOfMarkedCells())/2;
				B.unmarkColumn();
				return eval; // Winning column found: return immediately
			}
			
			B.unmarkColumn();
		}
		return eval;
	}

	public String playerName() {
		return "Rdy Player One";
	}
}
