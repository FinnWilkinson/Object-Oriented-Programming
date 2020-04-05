
public class Board {
<<<<<<< HEAD
	//Grid array (of players) and the next player//
	private Player[][] grid;
	private Player next;
	
	//Sets the constants for the number of rows, and the contents of the grid (X, O, or Blank)
	private static final int rows = 3, cols = 3;
	private static final Player Blank =Player.None;
	
	//Creates an empty board, and makes the next player X// 
	Board() {
		int r, c;
		for (r = 0; r < rows; r++) {
			for (c = 0; c < cols; c++) {
				grid[r][c] = Blank;
=======

	private static final int rows = 3, cols = 3;
	
	private Player[][] grid = new Player[rows][cols];
	private Player next;
	
	
	
	Board(){
		for(int i=0; i < rows; i++) {
			for(int j=0; j < cols; j++) {
				grid[i][j] = Player.None;
>>>>>>> 66baf52a864811de9e9dc7f41234902177d81e55
			}
		}
		next = Player.X;
	}
	
<<<<<<< HEAD
	//Takes the string input and turns it into a new position object//
	Position position(String s) {
		int r,c;
		char ch;
		ch = s.charAt(0);
		if ((s == null) | (s.length() != 2)) return null;
		if ((ch == 'a') | (ch == 'b') | (ch =='c')) {
			r = ch - 'a';
		}
		else return null;
		ch = s.charAt(1);
		if ((ch == '1') | (ch == '2') | (ch == '3')) {
			c = ch - '1';
		}
		else return null;
		if (grid[r][c] != Blank) return null;
		return new Position(r,c);
	}
	
	//Make the players move on the grid// 
	void move(Position position){
		grid[position.row()][position.col()] = next;
		next = next.other();
	}
	
	//Checks to see if there is a winner//
	Player winner() {
		int r, c;
		Player p; 
		
		for (r = 0; r < rows; r++) {
			for (c = 0; c < cols; c++) {
				if (grid[r][c] == Blank) return Blank;
			}
		}
		for (r = 0; r < rows; r++) {
			if ((grid[r][0] == grid[r][1]) && (grid[r][1] == grid[r][2])) {
				p = grid[r][0];
				return p;
			}
		}
		for (c = 0; c < cols; c++) {
			if ((grid[0][c] == grid[1][c]) && (grid[1][c] == grid[2][c])) {
				p = grid[0][c];
				return p;
			}
		}
		if ((grid[0][0] == grid[1][1]) && (grid[1][1] == grid[2][2])) {
			p = grid[0][0];
			return p;
		}
		else if ((grid[0][2] == grid[1][1]) && (grid[1][1] == grid[2][0])) {
			p = grid[1][1];
			return p;
		}
		return Player.Both;
	}
	
	Position[] blanks() {
		
	}
=======
	Position position(String s)
    {
        if (s == null || s.length() != 2) return null;
        char ch = s.charAt(0);
        int r = ch - 'a';
        if (r < 0 || r >= rows) return null;
        ch = s.charAt(1);
        int c = ch - '1';
        if (c < 0 || c >= cols) return null;
        if (grid[r][c] != Player.None) return null;
        Position pos = new Position(r,c);
        return pos;
    }
	
	void move(Position p) {
		if(grid[p.row()][p.col()] != Player.None) throw new Error ("Bad move.");
		grid[p.row()][p.col()] = next;
		next = next.other();
	}
	
	Player winner() {
		if(grid[0][0] == grid[1][0] & grid[0][0] == grid[2][0]) return grid[0][0];
		else if(grid[0][1] == grid[1][1] & grid[0][1] == grid[2][1]) return grid[0][1];
		else if(grid[0][2] == grid[1][2] & grid[0][2] == grid[2][2]) return grid[0][2];
		else if(grid[0][0] == grid[0][1] & grid[0][0] == grid[0][2]) return grid[0][0];
		else if(grid[1][0] == grid[1][1] & grid[1][0] == grid[1][2]) return grid[1][0];
		else if(grid[2][0] == grid[2][1] & grid[2][0] == grid[2][2]) return grid[2][0];
		else if(grid[0][0] == grid[1][1] & grid[0][0] == grid[2][2]) return grid[0][0];
		else if(grid[0][2] == grid[1][1] & grid[0][2] == grid[2][0]) return grid[0][2];
		
		
		for(int x=0; x < rows; x++) {
			for(int y=0; y < cols; y++) {
				if(grid[x][y] == Player.None) return Player.None;
			}
		}
		return Player.Both;
	}

	Position[] blanks() {
		Position[] temp = new Position[rows*cols];
		int k = 0;
		for(int i=0; i < rows; i++) {
			for(int j=0; j < cols; j++) {
				if(grid[i][j] == Player.None) {
					temp[k] = new Position(i,j);
					k = k+1;
				}
		
			}
		}
		Position[] blanks = new Position[k];
		for(int i=0; i < k; i++) blanks[i] = temp[i];
		return blanks;
	}
	
	public String toString() {
		String output = "";
		char c = 'a';
		output = output + "    1   2   3\n\n";
		for(int i=0; i < rows; i++) {
			output = output + c + "  ";
			for(int j=0; j < cols; j++) {
				if(j < cols - 1) {
					if(grid[i][j] == Player.None) output = output + " . |";
					else if(grid[i][j] == Player.O) output = output + " O |";
					else if(grid[i][j] == Player.X) output = output + " X |";
				}
				else{
					if(grid[i][j] == Player.None) output = output + " . ";
					else if(grid[i][j] == Player.O) output = output + " O ";
					else if(grid[i][j] == Player.X) output = output + " X ";
				}
			}
			if(i < rows - 1) {
				output = output + "\n   ---+---+--- \n";
			}
			else output = output + "\n";
			c++;
		}
		
		return output;
	}
	
>>>>>>> 66baf52a864811de9e9dc7f41234902177d81e55
}
