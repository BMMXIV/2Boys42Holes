package connectx.RP4;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import javax.swing.text.Position;

/**
 * 
 */
public class RP4 implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
    private CXGameState draw;
    //private CXGameState open;
    private Score score = new Score();
	private int  TIMEOUT;
	private long START;
	private int M;
	private int N;
	private int nodes;
	private double oneChipWeight;
	private double twoChipWeight;
	private double threeChipWeight;
	private int maxDepth;
	private boolean P1;

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
	public RP4() {
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
		oneChipWeight = 0.1;
		twoChipWeight = 0.3;
		threeChipWeight = 0.9;
		maxDepth = 10;
		P1 = first;
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
			score = evaluateColumns(B, L, -M*N, M*N, maxDepth, P1);
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

    private Score evaluateColumns(CXBoard B, Integer[] L, int alpha, int beta, int depth, boolean P) throws TimeoutException {
        Score eval = new Score();
        Score tmp = new Score();
		nodes++;
		//int alpha = -M*N;
		//int beta = M*N;

		//we know from the recursive call that the game is either drawn or still open
		//and if it's the first call of evaluateColumns and not a recursive one game is open for sure

		//we check if it's a draw
		if (B.gameState() == draw) {
            eval.val = 0;
			eval.alpha = Math.max(eval.val, eval.alpha);
            return eval;
        }
		//game is not drawn, so it must be open (can't be already won by a player)

		//check if there's a move to win immediately
        eval = singleMoveWin(B, L);
        if (eval.column != -1) {
			int temp = eval.alpha;
			eval.alpha = -eval.beta;
			eval.beta = -temp;
			eval.alpha = Math.max(eval.val, eval.alpha);
            return eval;
        }

		if (depth == 0) {
			//alpha and beta are the opposite of alpha and beta of the other player
			eval.alpha = -beta;
			eval.beta = -alpha;

			/**
			 * If we're here it means there was no win in one move,
			 * otherwise singleMoveWin() would have found it
			 */
			for(int i : L) {
				checktime(); // Check timeout at every iteration
				
				//after this move game is either still open or a draw
				//because there was no single move winning
				B.markColumn(i);
				tmp = evaluateColumns(B, B.getAvailableColumns(), eval.alpha, eval.beta, depth-1, !P);

				tmp.val = -tmp.val;		//score is the opposite of the score of the other player
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
		}
		else {
			eval.val = (int)Math.round(heuristicEval(B, P));
		}
		
        return eval;
    }

	/**
	 * Check number of chips' lines and how many chips long they are
	 * Consider only lines that can still get longer
	 * 
	 * Linear weighting function to calculate evaluation
	 */	
	private double heuristicEval(CXBoard B, boolean P){
		CXCellState me = P ? CXCellState.P1 : CXCellState.P2;
		CXCellState you = P ? CXCellState.P2 : CXCellState.P1;
		//number of 1, 2, and 3 consecutive chips
		Integer[] myConsecChips = {0, 0, 0};
		Integer[] yourConsecChips = {0, 0, 0};

		//check vertical lines
		for (int col = 0; col < N; col++) {
			int consecutiveChips = 0;
			CXCellState owner = CXCellState.FREE;
			CXCellState actCell;
			for (int row = M-2; row >= 0; row--) {
				actCell = B.cellState(row, col);
				if (owner == CXCellState.FREE) {
					if (actCell == me || actCell == you) {
						owner = actCell;
						consecutiveChips++;
					}
				}
				else if (owner == actCell) {
					consecutiveChips++;
				}
				else {
					break;
				}
			}
			if (consecutiveChips != 0){
				if (owner == me){
					myConsecChips[consecutiveChips - 1]++;
				}
				else{
					yourConsecChips[consecutiveChips - 1]++;
				}
			}
		}

		Integer[] horizontalChips;

		//check horizontal lines for me
		horizontalChips = checkHorizontalLines(B, P);
		for (int i = 0; i < 3; i++){
			myConsecChips[i] += horizontalChips[i];
		}

		//check horizontal lines for you
		horizontalChips = checkHorizontalLines(B, !P);
		for (int i = 0; i < 3; i++){
			yourConsecChips[i] += horizontalChips[i];
		}

		//check diagonal lines
		//TO DO

		double myPoints = myConsecChips[0]*oneChipWeight + myConsecChips[1]*twoChipWeight + myConsecChips[2]*threeChipWeight;
		double yourPoints = yourConsecChips[0]*oneChipWeight + yourConsecChips[1]*twoChipWeight + yourConsecChips[2]*threeChipWeight;
		
		//make heuristic evaluation comparable to leaf evaluation?
		return myPoints - yourPoints;
	}

	private Integer[] checkHorizontalLines(CXBoard B, boolean P){
		CXCellState me = P ? CXCellState.P1 : CXCellState.P2;
		CXCellState you = P ? CXCellState.P2 : CXCellState.P1;
		
		Integer[] myConsecChips = {0, 0, 0};

		for (int row = M-1; row >= 0; row--){
			int consecutiveChips = 0;
			boolean canGrow = false;
			CXCellState actCell = B.cellState(row, 0);
			if (actCell == me){
				consecutiveChips++;
			}
			else if (actCell == CXCellState.FREE){
				canGrow = true;
			}
			for (int col = 1; col < N-1; col++){
				actCell = B.cellState(row, col);
				if (actCell == me){
					consecutiveChips++;
				}
				else {
					if (actCell == CXCellState.FREE){
						canGrow = true;
					}
					if (canGrow && consecutiveChips > 0){
						myConsecChips[consecutiveChips - 1]++;
					}
					consecutiveChips = 0;
					if (actCell == you){
						canGrow = false;
					}
				}
			}
			actCell = B.cellState(row, N-1);
			if (actCell == me){
				consecutiveChips++;
			}
			else if (actCell == CXCellState.FREE){
				canGrow = true;
			}
			if (canGrow && consecutiveChips > 0){
				myConsecChips[consecutiveChips - 1]++;
			}
		}

		return myConsecChips;
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
		return "Visited Nodes: " + Integer.toString(nodes) +" Rdy Player 4";
	}
}
