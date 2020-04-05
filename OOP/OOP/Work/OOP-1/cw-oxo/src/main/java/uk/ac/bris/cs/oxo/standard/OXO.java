package uk.ac.bris.cs.oxo.standard;

import uk.ac.bris.cs.gamekit.matrix.Matrix;
import uk.ac.bris.cs.gamekit.matrix.SquareMatrix;
import uk.ac.bris.cs.oxo.Cell;
import uk.ac.bris.cs.oxo.Player;
import uk.ac.bris.cs.oxo.Side;
import uk.ac.bris.cs.oxo.Spectator;
import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.matrix.ImmutableMatrix;
import uk.ac.bris.cs.oxo.Outcome;

public class OXO implements OXOGame {
	
	private Player nought, cross;
	private Side currentSide;
	private int size;
	private SquareMatrix<Cell> matrix;
	

	public OXO(int size, Side startSide, Player nought, Player cross) {
		if (size <= 0) throw new IllegalArgumentException("size invalid");
		this.currentSide = requireNonNull(startSide);
		this.nought = requireNonNull(nought);
		this.cross = requireNonNull(cross);
		this.matrix = new SquareMatrix<Cell>(size, new Cell());
		
	}

	@Override
	public void registerSpectators(Spectator... spectators) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectators(Spectator... spectators) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void start() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Matrix<Cell> board() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Side currentSide() {
		// TODO
		throw new RuntimeException("Implement me");
	}
}
