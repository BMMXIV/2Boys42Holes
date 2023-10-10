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
public class RP1old implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
    private CXGameState draw;
    private CXGameState open;
	private int  TIMEOUT;
	private long START;

	/* Default empty constructor */
	public RP1old() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        draw = CXGameState.DRAW;
        open = CXGameState.OPEN;
		TIMEOUT = timeout_in_secs;
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
		int save    = L[rand.nextInt(L.length)]; // Save a random column 

		try
		{
			Integer[] state = evaluateColumns(B, L, true);
			return state[1];
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

    private Integer[] evaluateColumns(CXBoard B, Integer[] L, Boolean P) throws TimeoutException
    {
		//currentState[0] -> state, currentState[1] -> column chosen
        Integer[] currentState = {-1, -1};
		Integer[] stateInt = {-1, -1};

		if (P)		//it's my turn, i consider the state losing at the start
			currentState[0] = 0;
		else		//adversary's turn
			currentState[0] = 2;

		int col = singleMoveWin(B, L, P);
		if(col != -1)
		{
			if(P)
				currentState[0] = 2;
			else
				currentState[0] = 0;
			currentState[1] = col;
			return currentState;
		}

        for(int i : L) // compute the score of all possible next move and keep the best one
        {
			checktime(); // Check timeout at every iteration

            CXGameState state = B.markColumn(i);
            if (state == open)
            {
				Integer[] L1 = B.getAvailableColumns();
                stateInt = evaluateColumns(B, L1, !P);
            }
			else
			{
				if (state == yourWin)
					stateInt[0] = 0;
				else if (state == draw)
					stateInt[0] = 1;
				else
					stateInt[0] = 2;
			}
            B.unmarkColumn();

			if (P)	//my turn, maximizing (going towards myWin)
			{
				if (stateInt[0] >= currentState[0])
				{
					currentState[0] = stateInt[0];
					currentState[1] = i;	//set i as column chosen, found a better move
				}
				if (currentState[0] == 2)	//alreay found move to win
					break;
			}
			else	//other player's turn, minimizing (going towards yourWin)
			{
				if (stateInt[0] <= currentState[0])
				{
					currentState[0] = stateInt[0];
					currentState[1] = i;	//set i as column chosen, found a better move
				}
				if (currentState[0] == 0)	//alreay found move to win
					break;
			}
        }

        return currentState;
    }

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private int singleMoveWin(CXBoard B, Integer[] L, Boolean P) throws TimeoutException {
    for(int i : L) {
			checktime(); // Check timeout at every iteration
      CXGameState state = B.markColumn(i);
      if ((P && state == myWin) || (!P && state == yourWin))
        return i; // Winning column found: return immediately
      B.unmarkColumn();
    }
		return -1;
	}

	public String playerName() {
		return "!Ready P1";
	}
}
