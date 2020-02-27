package sudoku;

public class Solver {
	private static byte[][] START_GRID = 
	{
		{0,0,0, 0,0,0, 0,1,2},	
		{0,0,0, 0,0,0, 0,0,3},
		{0,0,2, 3,0,0, 4,0,0},
		
		{0,0,1, 8,0,0, 0,0,5},
		{0,6,0, 0,7,0, 8,0,0},
		{0,0,0, 0,0,9, 0,0,0},
		
		{0,0,8, 5,0,0, 0,0,0},
		{9,0,0, 0,4,0, 5,0,0},
		{4,7,0, 0,0,6, 0,0,0}
	};	

	private byte[][] grid;
	
	private boolean possibilities[][][];
	private byte workGrid[][];
	private byte unsolvedCells = 81;
	private int depth = 0;
	private static long startTime;
	public static void main(String[] args){
		startTime = System.currentTimeMillis();
		Solver sudoku = new Solver(START_GRID,0);
		if(sudoku.solve() == false){
			System.out.println("Unable to solver the given grid");
		}
		sudoku.printGrid();
		System.out.println("Took: " + (System.currentTimeMillis()-startTime) + " milliseconds.");
	}
	
	private Solver(byte[][] grid, int depth){
		this.grid = grid;
		this.depth = depth;
	}
	
	private boolean solve(){
		initializeGrids();
		unsolvedCells = countUnsolved();
		while(unsolvedCells > 0){
			if(isSolvable() == false || isValid() == false){ //isValid checks for duplicates
				return false;
			}
			byte added = round();
			unsolvedCells -= added;
		}
		return isValid();
	}
	
	private boolean isSolvable(){
		for(byte i = 0; i<9; i++){
			for(byte j = 0; j<9; j++){
				if(countPossibilities(i,j) == 0){//if there are no possibilities left, you can't solve the sudoku
					return false;
				}
			}	
		}
		return true;
	}

	private boolean isValid(){
		boolean[] duplicates = new boolean[9];
		
		//rows
		for(byte i = 0; i<9; i++){
			for(byte j = 0; j<9; j++){
				if(workGrid[i][j] != 0){
					byte number = (byte) (workGrid[i][j]-1);
					if(duplicates[number] == false){
						duplicates[number] = true;
					}else{
						return false;
					}
				}

			}
		}
		//columns
		duplicates = new boolean[9];
		for(byte i = 0; i<9; i++){
			for(byte j = 0; j<9; j++){
				if(duplicates[workGrid[j][i]-1] == false){
					duplicates[workGrid[j][i]-1] = true;
				}else{
					return false;
				}
			}
		}
		
		//boxes
		duplicates = new boolean[9];
		for(byte i = 0; i<3; i++){
			for(byte j = 0; j<3; j++){
				for(byte k = 0; k<9; k++){
					if(duplicates[workGrid[i*3+k/3][j*3%3]] == false){
						duplicates[workGrid[i*3+k/3][j*3+k%3]] = true;
					}else{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private byte round(){
		solveRowColumnBox();
		
		byte added = updateWorkGrid();//returns how many new numbers are known
		if(added == 0){//go more complex if other methods fail
			solveHidden();
			added = updateWorkGrid();
			if(added == 0 ){//brute force :D
				bingo();// doesn't work yet :( 
				added = updateWorkGrid();
				System.out.println("used bruteforce to add: "+added);
			}	
		}
		return added;
	}
	
	private void initializeGrids() {
		workGrid = new byte [9][9];
		possibilities = new boolean [9][9][9];
		
		for(byte i = 0;i<9;i++){
			for(byte j = 0; j<9; j++){
				workGrid[i][j] = 0;
			}
		}	
		
		for(byte i = 0;i<9;i++){
			for(byte j = 0; j<9; j++){
				for(byte k = 0;k<9;k++){//go through the entire possibilities array, use Grid to fill it
					if(grid[i][j] == (k+1)){
						for(byte m = 0; m<9;m++){//already removing possibilities, because that cell is 8 it can't be any other
							if( m==k){
								possibilities[i][j][m] = true;
								workGrid[i][j] = (byte) (k+1);
							}else{
								possibilities[i][j][m] = false;
							}
						}
					}else{
						possibilities[i][j][k] = true;
					}
						
				}
			}
		}
	}
	
	private byte countUnsolved(){
		byte unsolved = 0;
		for(byte i = 0;i<9;i++){
			for(byte j = 0; j<9; j++){
				if(grid[i][j]==0){
					unsolved++;
				}
			}
		}	
		return unsolved;
	}
	
	private void printGrid(){
		for(byte i=0; i<9; i++){
			System.out.print("{");
			for (byte j=0; j<9; j++){
				System.out.print("[");
				System.out.print(workGrid[i][j]);
				System.out.print("] ");
			}
			System.out.println("}");
		}
	}
	
	private void solveRowColumnBox(){
		for(byte i = 0;i<9;i++){
			for(byte j = 0; j<9; j++){
				if(workGrid[i][j] != 0){ //plenty of room for improvement here
					
					byte number = (byte) (workGrid[i][j]-1);
					byte boxX = (byte) (i/3);
					byte boxY = (byte) (j/3);
					
					for(byte k = 0;k<9;k++){//removing everything in same row and column
						if(k != i){
							possibilities[k][j][number] = false; //row
						}
						
						if(k != j){
							possibilities[i][k][number] = false; //column
						}
					}
					
					for(byte a = 0; a<3; a++){//everything in tiny Box
						for(byte b = 0; b<3; b++){
							if( !((boxX*3+a == i) && (boxY*3+b == j))){//go java hotspot
								possibilities[boxX*3+a][boxY*3+b][number] = false;
							}
						}
					}
				}
			}
		}	
	}
	
	private void solveHidden(){
		for(byte number = 0; number<9; number++){
		
			for(byte i=0; i<9; i++){//rows
				byte count = 0;
				byte possible = 0;
				for(byte j=0; j<9;j++){
					if(possibilities[i][j][number] == true){
						count++;
						possible = j;
					}
				}
				if(count == 1 && workGrid[i][possible] == 0){//if only 1, set every possibility except that one false
					for(byte k=0; k<9;k++){
						if(k != number){
							possibilities[i][possible][k] = false;
						}		
					}
				}
			}
			
			for(byte i=0; i<9; i++){//columns
				byte count = 0;
				byte possible = 0;
				for(byte j=0; j<9;j++){
					if(possibilities[j][i][number] == true){
						count++;
						possible = j;
					}
				}
				if(count == 1 && workGrid[possible][i] == 0){//if only 1, set every possibility except that one false
					for(byte k=0; k<9;k++){
						if(k != number){
							possibilities[possible][i][k] = false;
						}		
					}
				}
			}
			
			for(byte boxX=0; boxX<3; boxX++){//boxes
				for(byte boxY=0; boxY<3; boxY++){
					byte count = 0;
					byte possible = 0;
					for(byte j=0; j<9;j++){
						if(possibilities[boxX*3+(j/3)][boxY*3+(j%3)][number] == true){
							count++;
							possible = j;
						}
					}
					if(count == 1){//if only 1, set every possibility except that one false
						for(byte k=0; k<9;k++){
							if(k != number){
								possibilities[boxX*3+(possible/3)][boxY*3+(possible%3)][k] = false;
							}		
						}
					}
				}
			}
		}
	}


    //This function will try a random number, and try to solve the sudoku with that extra digit
    //This function is not thoroughly tested, and most likely not working	
	private void bingo(){
		byte lowest = Byte.MAX_VALUE;
		byte I = 0;
		byte J = 0;
		for(byte i = 0; i<9; i++){//finding least amount of possibilities
			for(byte j = 0; j<9; j++){
				byte number = countPossibilities(i,j);
				if(number < lowest && number != 1){
					lowest = number;
					I = i;
					J = j;
				}
			}
		}
		
		
		boolean running = true;
		while(running){
			byte[][] tempGrid = workGrid.clone();
			byte possible=0;
			for(byte k = 0;k<9;k++){//find one number
				if(possibilities[I][J][k] == true){
					possible = (byte) (k+1);
					break;
				}
			}
			tempGrid[I][J] = possible;
			System.out.println("depth: "+ depth);
			Solver subsolution = new Solver(tempGrid,depth + 1);
			
			if(subsolution.solve() == false){//one of them should return true
				possibilities[I][J][possible-1] = false;
			}else{
				for(byte k = 0; k<9; k++){//this is the right solution, everything else is false
					if(k != (possible-1)){
						possibilities[I][J][k] = false;
					}
				}
				running = false;
			}
		}
	}
	
	private byte countPossibilities(byte i, byte j){
		byte counter = 0;
		for(byte k = 0; k<9; k++){
			if(possibilities[i][j][k] == true){
				counter++;
			}
		}
		return counter;
	}
	
	private byte updateWorkGrid(){
		byte added=0;
		for(byte i = 0;i<9;i++){
			for(byte j = 0; j<9; j++){
				if(workGrid[i][j] == 0){//if a cell is unknown
					byte counter = 0;
					byte number = 0;
					for(byte k = 0;k<9;k++){//check how many possibilities that cell has
						if(possibilities[i][j][k] == true){
							counter++;
							number = (byte) (k+1);
						}
					}
					if(counter == 1){//if it's only 1, set it
						workGrid[i][j] = number;
						added++;
					}
					counter = 0;
				}	
			}
		}
		return added;
	}
}
