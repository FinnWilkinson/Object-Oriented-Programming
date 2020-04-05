package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.MoveVisitor;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

// TODO name the AI
@ManagedAI("Mr X AI")
public class MrXAI implements PlayerFactory{

	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player, MoveVisitor{
		private TicketMove tempTicketMove;
		
		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			final List<Move> origionalMoves = new ArrayList<Move>(moves);
			final List<Colour> players = view.getPlayers();
			Move moveToMake = origionalMoves.get(0);
			double tempScore = 0;
			double score = 0;
			double multiplier = 0;
			//iterates through all valid moves in the given list
			for (Move possibleMove : moves) {
				possibleMove.visit(this);
				//gets multiplier value dependant on what type of node it is i.e. TAXI, UNDERGROUND
				multiplier = multiplierSetter(view);
				//our scoring algorithm
				tempScore = (multiplier * bredthFirstSearch(tempTicketMove.destination(), players, view)) 
							* (numberOfMovesAvailableFromNewLocation(view, location, players));
				if (tempScore > score) {
					score = tempScore;
					moveToMake = possibleMove;
				}				
			}		
			callback.accept(moveToMake);
		}
		
		@Override
		public void visit(DoubleMove move) {
			visit(move.secondMove());
		}
		
		@Override
		public void visit(TicketMove move) {
			tempTicketMove = move;
		}
		
		//dependant on the edge data values coming from a given node, returns the appropriate multiplier value
		private double multiplierSetter(ScotlandYardView view) {
			double multiplier = 0;
			final Node<Integer> tempMoveNode = new Node<Integer>(tempTicketMove.destination());
			final Collection<Edge<Integer, Transport>> tempEdges = view.getGraph().getEdgesFrom(tempMoveNode);
			for(Edge<Integer, Transport> edge : tempEdges) {
				switch(edge.data()) {
				case TAXI:
					multiplier = 2;
					break;
				case BUS:
					multiplier = 2.5;
					break;
				case UNDERGROUND:
					multiplier = 3;
					break;
				case FERRY:
					multiplier = 3;
					break;
				}
				
			}
			return multiplier;
		}
		
		//works out the shortest distance to the nearest detective
		private int bredthFirstSearch(int moveLocation, List<Colour> players, ScotlandYardView view) {
			final ImmutableGraph<Integer, Transport> ourGraph = new ImmutableGraph<>(view.getGraph());
			final Node<Integer> startNode = new Node<Integer>(moveLocation);
			final List<Node<Integer>> targetNodes = new ArrayList<Node<Integer>>();
			//create node for each detective and add to targetNodes list
			for (Colour player : players) {
				if (player != BLACK) {
					final Node<Integer> temp = new Node<Integer>(view.getPlayerLocation(player).get());
					targetNodes.add(temp);
				}
			}
			final List<Node<Integer>> visited = new ArrayList<Node<Integer>>();
			//add neighbouring nodes to move location
			final List<Node<Integer>> neighbours = new ArrayList<Node<Integer>>(addNeighbourNodes(ourGraph, startNode));
			boolean detFound = false;
			int countSteps = 0;
			//if neighbouring node has a detectives on it, return count value.
			//otherwise get all neighbouring nodes of current neighbouring nodes and repeat
			while(detFound == false) {
				countSteps ++;
				visited.addAll(neighbours);
				neighbours.clear();
				for(Node<Integer> node : targetNodes) {
					if (visited.contains(node)) detFound = true;
				}
				for(Node<Integer> node : visited) {
					neighbours.addAll(addNeighbourNodes(ourGraph, node));
				}
			}	
			return countSteps;
		}
		
		//Adds nodes connected to the given node to a list, and returns this
		private List<Node<Integer>> addNeighbourNodes(Graph<Integer, Transport> ourGraph, Node<Integer> sourceNode){
			requireNonNull(sourceNode);
			final List<Node<Integer>> temp = new ArrayList<Node<Integer>>();
			final Collection<Edge<Integer, Transport>> edges = ourGraph.getEdgesFrom(sourceNode);
			for (Edge<Integer,Transport> edge : edges) {
				temp.add(edge.destination());
			}
			return temp;
		}
		
		//gets and returns the number of moves available from the new location
		private int numberOfMovesAvailableFromNewLocation(ScotlandYardView view, int location, List<Colour> players) {
			final int numberOfAvailableMoves = ticketMoves(location, view, players).size();
			return numberOfAvailableMoves;
		}		
		
		//creates and returns all the valid ticket moves that can be made
		private Set<Move> ticketMoves(Integer location, ScotlandYardView view, List<Colour> players){	
			final Set<Move> allTicketMoves = new HashSet<>();
			final Node<Integer> sourceNode = new Node<Integer>(location);
			//get all connected edges from current player location
			final Collection<Edge<Integer, Transport>> connectedEdges = view.getGraph().getEdgesFrom(sourceNode);		
			for(Edge<Integer, Transport> x : connectedEdges) {
				//check to see if move location is empty
				if (checkNodeIsEmpty(x, players, view)) {
					//check mrX has a ticket to move there
					if(view.getPlayerTickets(BLACK, Ticket.fromTransport(x.data())).get() != 0) {
						//create and add ticket move to list
						final TicketMove temp = new TicketMove(BLACK, Ticket.fromTransport(x.data()), x.destination().value());
						allTicketMoves.add(temp);
					}
					if(view.getPlayerTickets(BLACK, SECRET).get() != 0) {
						//create and add secret move for each ticket move
						final TicketMove secretTemp = new TicketMove(BLACK, Ticket.SECRET, x.destination().value());
						allTicketMoves.add(secretTemp);
					}
				}
			}		
			return allTicketMoves;
		}
		
		//checks to see if a destination node is currently occupied
		private boolean checkNodeIsEmpty(Edge<Integer, Transport> edge, List<Colour> players, ScotlandYardView view) {
			for (Colour x : players) {
				if (x != BLACK) {
					if (view.getPlayerLocation(x).get() == edge.destination().value()) return false;
				}
			}
			return true;
		}
	}
}