package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;



// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {
	
	private List<Boolean> rounds;
	private ImmutableGraph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private Collection<Spectator> spectators = new ArrayList<Spectator>();
	private int currentPlayer = 0;
	private int currentRound = 0;
	private int mrXLastLoc = 0;
	private Set<Move> moves;
	private int noOfRounds;
	private Set<Colour> winningPlayers = new HashSet<>();
	private boolean detCaughtMrX = false;	
	
	//initialising the model, making sure parameters have the correct value,
	//and are not NULL
	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		this.graph = new ImmutableGraph<>(graph);
		this.rounds = new ArrayList<>(requireNonNull(rounds));
		if(rounds.isEmpty()) throw new IllegalArgumentException("Empty rounds");
		if(graph.isEmpty()) throw new IllegalArgumentException("Empty graph");
		//make sure MrX's colour = BLACK
		if (mrX.colour != BLACK) throw new IllegalArgumentException("MrX should be Black");
		//Create and list of all players
		final ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		configurations.add(0, mrX);
		configurations.add(0, firstDetective);
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			configurations.add(0,requireNonNull(configuration));
		}		
		//test for duplicate locations and colours
		testLocationsAndColour(configurations);
		//test to see if the detectives have any double or secret tickets, which they should not. 
		//Also checks that no tickets are missing for all players
		testTickets(configurations);
		//converts all players to type ScotlandYardPlayer
		ConfigurationToScotlandYardPlayer(configurations, players);
		noOfRounds = rounds.size();
	}
	
	//tests for duplicate locations or colour between all players. if any appear twice an appropriate exception is thrown
	private void testLocationsAndColour(ArrayList<PlayerConfiguration> configurations) {
		//test for duplicate colours
		final Set<Colour> ColourSet = new HashSet<>();
		for(PlayerConfiguration configuration : configurations) {
			if (ColourSet.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			ColourSet.add(configuration.colour);
		}		
		//test for duplicate locations
		final Set<Integer> LocationSet = new HashSet<>();
		for(PlayerConfiguration configuration : configurations) {
			if (LocationSet.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			LocationSet.add(configuration.location);
		}
	}
	
	//Ensures that the correct players have the correct tickets
	private void testTickets(ArrayList<PlayerConfiguration> configurations) {
		for(PlayerConfiguration c : configurations) {
			//Does MrX tests
			if(c.colour == BLACK) {
				for(Ticket t : Ticket.values()) {
					if(c.tickets.get(t) == null)
						throw new IllegalArgumentException("MrX is missing ticket type" + t);
				}
			}
			//Does tests for detectives
			else {
				for(Ticket t : Ticket.values()) {
					//makes sure detectives don't have DOUBLE or SECRET tickets
					if(t == DOUBLE || t == SECRET) {
						if(c.tickets.get(t) != 0) {
							throw new IllegalArgumentException("Detective " + c.colour + " should not have any " + t + " tickets");
						}
					}
					//Makes sure detectives have TAXI, BUS, UNDERGROUND tickets
					else {
						if(c.tickets.get(t) == null) {
							throw new IllegalArgumentException("Detective " + c.colour + " should have " + t + " tickets");
						}
					}
				}
			}
		}
	}
	
	//convert each player of type PlayerConfiguration to ScotlandYardPlayer so that we have mutable instances
	//this is useful for changing location and ticket values. this is not possible in PlayerConfiguration class
	private void ConfigurationToScotlandYardPlayer(ArrayList<PlayerConfiguration> configurations, List<ScotlandYardPlayer> players) {
		for(PlayerConfiguration c : configurations) {
			final ScotlandYardPlayer tempPlayer = new ScotlandYardPlayer(c.player, c.colour, c.location, c.tickets);
			players.add(0, tempPlayer);
		}		
	}
	
	//this adds a spectator to the list of spectators
	@Override
	public void registerSpectator(Spectator spectator) {
		if (spectator == null) throw new NullPointerException("Require non-null specatator");
		if(spectators.contains(spectator)) {
			throw new IllegalArgumentException("Spectator already in registered");
		}
		spectators.add(spectator);
	}
	
	//unregisters a spectator from the list of spectators
	@Override
	public void unregisterSpectator(Spectator spectator) {
		if (spectator == null) throw new NullPointerException("Require non-null spectator");
		if (!spectators.contains(spectator)) throw new IllegalArgumentException("Spectator does not exist");
		spectators.remove(spectator);
	}
	
	//returns a set of valid moves a player can make
	private Set<Move> validMove (Colour player, Integer location) {
		moves = new HashSet<>();
		final PassMove pass = new PassMove(player);
		moves.addAll(ticketMoves(location));
		if (currentRound < (noOfRounds - 1)) {
			if (currentPlayer == 0) {
				if (players.get(currentPlayer).hasTickets(DOUBLE)) {
					moves.addAll(doubleMoves(location));
				}
			}
		}
		if (currentPlayer != 0) {
			if (moves.isEmpty()) moves.add(pass);
		}
		return Collections.unmodifiableSet(moves);
	}
	
	
	//creates and returns all the valid ticket moves that can be made
	private Set<TicketMove> ticketMoves(Integer location){
		final ScotlandYardPlayer current = players.get(currentPlayer);		
		final Set<TicketMove> allTicketMoves = new HashSet<>();
		final Node<Integer> sourceNode = new Node<Integer>(location);
		//get all edges from current node
		final Collection<Edge<Integer, Transport>> connectedEdges = graph.getEdgesFrom(sourceNode);		
		for(Edge<Integer, Transport> x : connectedEdges) {
			//check each connected node is empty
			if (checkNodeIsEmpty(x)) {
				//check player has enough tickets
				if(current.hasTickets(Ticket.fromTransport(x.data()))) {
					//create ticket move for this location and add to ticket move list
					final TicketMove temp = new TicketMove(current.colour(), Ticket.fromTransport(x.data()), x.destination().value());
					allTicketMoves.add(temp);
				}
				if (currentPlayer == 0) {
					//if mrX, add a secret move for each ticket move, if mrX has a secret ticket
					if(current.hasTickets(SECRET)) {
						final TicketMove secretTemp = new TicketMove(current.colour(), Ticket.SECRET, x.destination().value());
						allTicketMoves.add(secretTemp);
					}
				}
			}
		}		
		return allTicketMoves;
	}
	
	//returns a set of valid double moves a that can be made
	private Set<Move> doubleMoves(Integer location){
		final ScotlandYardPlayer current = players.get(currentPlayer);		
		final Set<Move> allDoubleMoves = new HashSet<>();
		final Node<Integer> sourceNode = new Node<Integer>(location);
		final Collection<Edge<Integer, Transport>> firstConnectedEdges = graph.getEdgesFrom(sourceNode);		
		for (Edge<Integer, Transport> x : firstConnectedEdges) {
			//checks if each node is empty (no other player is occupying it)
			if (checkNodeIsEmpty(x)) {
				//check current player has the appropriate ticket
				if (current.hasTickets(Ticket.fromTransport(x.data()))) {
					//for each node that is connected to this first node
					for (Edge<Integer, Transport> y : graph.getEdgesFrom(x.destination())) {
						//check node is unoccupied
						if (checkNodeIsEmpty(y)) {
							//if ticket type is same for both travels, check player has 2 tickets of that type
							if (x.data() == y.data()) {
								if (current.hasTickets(Ticket.fromTransport(y.data()), 2)){
									final DoubleMove temp = new DoubleMove(current.colour(), Ticket.fromTransport(x.data()),
											x.destination().value(), Ticket.fromTransport(y.data()), y.destination().value());
									allDoubleMoves.add(temp);
								}
							}
							//otherwise, check player has type for second move
							else if (current.hasTickets(Ticket.fromTransport(y.data()))) {
								final DoubleMove temp = new DoubleMove(current.colour(), Ticket.fromTransport(x.data()), 
										x.destination().value(), Ticket.fromTransport(y.data()), y.destination().value());
								allDoubleMoves.add(temp);
							}
							//checks player has secret tickets available
							if (current.hasTickets(SECRET)) {
								final DoubleMove secretFirst = new DoubleMove(current.colour(), Ticket.SECRET, 
										x.destination().value(), Ticket.fromTransport(y.data()), y.destination().value());
								allDoubleMoves.add(secretFirst);
								final DoubleMove secretSecond = new DoubleMove(current.colour(), Ticket.fromTransport(x.data()), 
										x.destination().value(), Ticket.SECRET, y.destination().value());
								allDoubleMoves.add(secretSecond);
								//checks if player has 2 secret tickets, if so, adds an additional double move
								if (current.hasTickets(SECRET, 2)) {
									final DoubleMove secretBoth = new DoubleMove(current.colour(), Ticket.SECRET, x.destination().value(), 
											Ticket.SECRET, y.destination().value());
									allDoubleMoves.add(secretBoth);
								}
							}
						}
					}
				}
			}
		}
		return allDoubleMoves;		
	}
	
	//checks to see if a destination node is currently occupied
	private boolean checkNodeIsEmpty(Edge<Integer, Transport> edge) {
		for (ScotlandYardPlayer x : players) {
			if (x.colour() != BLACK) {
				if (x.location() == edge.destination().value()) return false;
			}
		}
		return true;
	}
	
	//when the round begins, gets valid moves for fist player, and passes control to the accept method
	@Override
	public void startRotate() {
		if(isGameOver()) throw new IllegalStateException("Game is over");
		final ScotlandYardPlayer current = players.get(currentPlayer); 
		moves = validMove(current.colour(), current.location());
		current.player().makeMove(this, current.location(), moves, this);
		
	}
	
	//returns a collection of spectators
	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(spectators);
	}
	
	//returns the list of players in the game
	@Override
	public List<Colour> getPlayers() {
		final List<Colour> playerColours = new ArrayList<>();
		for(ScotlandYardPlayer x : this.players) {
			playerColours.add(x.colour());
		}
		return Collections.unmodifiableList(playerColours);
	}
	
	//returns the list of winning players
	@Override
	public Set<Colour> getWinningPlayers() {
		isGameOver();
		return Collections.unmodifiableSet(winningPlayers);
	}
	
	//returns a player location
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if (colour == BLACK) {
			return Optional.of(mrXLastLoc);
		}
		else {
			for(ScotlandYardPlayer x : players) {
				if(x.colour() == colour) return Optional.of(x.location());
			}
		}
		return Optional.empty();
	}
	
	//returns the number of tickers a player has for a particular type of ticket
	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer x : players) {
			if (x.colour() == colour) {
				return Optional.of(x.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}
	
	//performs the checks to see if the game is over
	@Override
	public boolean isGameOver() {
		int numberOfTicketsLeft = 0;
		//if all rounds are played, mrX wins
		if (currentRound == noOfRounds && currentPlayer == 0) {
			addWinningPlayers(true, false);
			return true;
		}
		for(ScotlandYardPlayer x : players) {
			if(x.colour() != BLACK) {
				//if a detective is in the same position as mrX, detectives win
				if(x.location() == players.get(0).location()) {
					addWinningPlayers(false, true);
					return true;
				}
				//checks to see if all detectives are out of all tickets
				for (Ticket t : Ticket.values()) {
					numberOfTicketsLeft = numberOfTicketsLeft + x.tickets().get(t);
				}
			}
			else {				
				//if mrX can't move, detectives win
				if (mrXCanMove(x) == false) {
					addWinningPlayers(false, true);
					return true;
				}
			}
		}
		//if all detectives are out of tickets, mrX wins
		if (numberOfTicketsLeft == 0) {
			addWinningPlayers(true, false);
			return true;
		}
		return false;
	}
	
	//checks to see if mrX can move at all. as isGameOver is checked at the end of the round, 
	//it accounts for additional tickets being given to mrX, and detectives moving around the board
	public boolean mrXCanMove(ScotlandYardPlayer player) {
		final Node<Integer> sourceNode = new Node<Integer>(player.location());
		final Collection<Edge<Integer, Transport>> connectedEdge = graph.getEdgesFrom(sourceNode);
		for (Edge<Integer, Transport> edge : connectedEdge) {
			if (checkNodeIsEmpty(edge)){
				if (player.hasTickets(Ticket.fromTransport(edge.data()))) return true;
				else if (player.hasTickets(SECRET)) return true;
			}
		}
		return false;
	}
	
	//adds appropriate winning players to the winning players list
	public void addWinningPlayers(boolean mrXWins, boolean detsWin) {
		if (mrXWins) {
			winningPlayers.add(BLACK);
		}
		else if (detsWin) {
			for (ScotlandYardPlayer x : players) {
				if (x.colour() != BLACK) winningPlayers.add(x.colour());
			}
		}
	}
	
	//returns the colour of the current player
	@Override
	public Colour getCurrentPlayer() {
		return players.get(currentPlayer).colour();
	}
	
	//returns the current round
	@Override
	public int getCurrentRound() {
		return currentRound;
	}
	
	//returns an immutable list<boolean> representing the rounds, true if round is a reveal round
	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}
	
	@Override
	public Graph<Integer, Transport> getGraph() {
		return graph;
	}

	//accept method
	@Override
	public void accept(Move t) {
		//checks move is non-null, and that it is in the valid set of moves
		requireNonNull(t);
		if (!moves.contains(t)) throw new IllegalArgumentException("Invalid Move");
		//calls the visit method of Move
		t.visit(this);
		if (currentPlayer == 0 || detCaughtMrX) {
			if (isGameOver()) {
				for (Spectator x : spectators) {
					x.onGameOver(this, winningPlayers);
				}
			}
			else {
				for (Spectator  x : spectators) {
					x.onRotationComplete(this);
				}
			}
		}
		//prompt the next player to make their move 
		else{
			final ScotlandYardPlayer current = players.get(currentPlayer); 
			moves = validMove(current.colour(), current.location());
			current.player().makeMove(this, current.location(), moves, this);
		}		
	}
	
	//visit method for if the move is a DoubleMove
	@Override
	public void visit(DoubleMove move) {
		//increase current player and remove a double move ticket from mrX
		currentPlayer = 1;
		players.get(0).removeTicket(DOUBLE);
		final DoubleMove tempMove = newDoubleMove(move);		
		//updates spectators that a double move is being made
		for (Spectator x : spectators) {
			x.onMoveMade(this, tempMove);
		}		
		final ScotlandYardPlayer current = players.get(0);
		//makes the first move
		current.location(move.firstMove().destination());
		current.removeTicket(move.firstMove().ticket());
		//updates mrX last location if it is a reveal round
		if(rounds.get(currentRound)) {
			mrXLastLoc = current.location();
		}
		//increase round and update spectators on new round and move made
		currentRound ++;
		for (Spectator x : spectators) {
			x.onRoundStarted(this, currentRound);
		}
		for (Spectator x : spectators) {
			x.onMoveMade(this, tempMove.firstMove());
		}
		//perform second move
		current.location(move.secondMove().destination());
		current.removeTicket(move.secondMove().ticket());
		//updates mrX last location if it is a reveal round
		if(rounds.get(currentRound)) {
			mrXLastLoc = current.location();
		}
		//increase round and update spectators on new round and move made
		currentRound ++;
		for (Spectator x : spectators) {
			x.onRoundStarted(this, currentRound);
		}
		for (Spectator x : spectators) {
			x.onMoveMade(this, tempMove.secondMove());
		}
	}
	
	//a new doubleMove is made, which makes mrX's destinations available to spectators depending on if it is one or two reveal rounds for the double move
	public DoubleMove newDoubleMove(DoubleMove move) {
		DoubleMove tempMove;
		if (rounds.get(currentRound) && rounds.get(currentRound + 1)) {
			tempMove = move;
		}
		else if (rounds.get(currentRound) && !rounds.get(currentRound + 1)) {
			tempMove = new DoubleMove(move.colour(), move.firstMove().ticket(), move.firstMove().destination(), 
					move.secondMove().ticket(), move.firstMove().destination());
		}
		else if (!rounds.get(currentRound) && rounds.get(currentRound + 1)){
			tempMove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastLoc, move.secondMove().ticket(), 
					move.secondMove().destination());
		}
		else {
			tempMove = new DoubleMove(move.colour(), move.firstMove().ticket(), mrXLastLoc, move.secondMove().ticket(), mrXLastLoc);
		}
		return tempMove;
	}
	
	//visit method for if the move is a PassMove
	@Override
	public void visit(PassMove move) {
		if (currentPlayer == players.size()-1) currentPlayer = 0;
		else currentPlayer++;
		for (Spectator x : spectators) {
			x.onMoveMade(this, move);
		}
	}
	
	//visit method for if the move is a TicketMove
	@Override
	public void visit(TicketMove move) {
		final int prevPlayer;		
		TicketMove tempMove = move;
		if (currentPlayer == players.size()-1) {
			prevPlayer = currentPlayer;
			currentPlayer = 0;		
			}
		else {
			prevPlayer = currentPlayer;
			currentPlayer ++;
		}
		//gets current player
		ScotlandYardPlayer current = players.get(prevPlayer);
		//make the move
		current.location(move.destination());
		current.removeTicket(move.ticket());
		//if its mrX, update last know location if appropriate
		if (current.colour() == BLACK) {
			if(rounds.get(currentRound)) {
				mrXLastLoc = current.location();
			}
			else {
				//if it is not a reveal round, make a new TicketMove so that spectators don't know what moves he can do
				tempMove = new TicketMove(move.colour(), move.ticket(), mrXLastLoc);
			}
			//increment current round and tell spectators
			currentRound ++;
			for (Spectator x : spectators) {
				x.onRoundStarted(this, currentRound);
			}
		}
		//if not mrX give mrX appropriate extra ticket
		else {
			ScotlandYardPlayer mrX = players.get(0);
			mrX.addTicket(move.ticket());
		}
		//tell spectators about move
		for (Spectator x : spectators) {
			//tempMove is always returned as if it is a detective, the move will remain unchanged, so this effects nothing.
			x.onMoveMade(this, tempMove);
		}
		//see if detective has just moved onto mrX location
		if(current.isDetective()) {
			if(current.location() == players.get(0).location()) detCaughtMrX = true;
		}
	}
}
