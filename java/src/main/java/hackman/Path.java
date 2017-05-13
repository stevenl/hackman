package hackman;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import move.Move;

/**
 * hackman.Path
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class Path {

    private Point start;
    private Point end;
    private List<Move> moves;

    public Path(Point start) {
        this.start = start;
        this.end   = start;
        this.moves = new ArrayList<>();
    }

    public Path(Point start, Point end, List<Move> moves) {
        this.start = start;
        this.end   = end;
        this.moves = moves;
    }

    public Point start() {
        return this.start;
    }
    public Point end() {
        return this.end;
    }
    public List<Move> moves() {
        return this.moves;
    }

    public Path addMove(Move move) {
        Point end = _movePoint(this.end, move);
        List<Move> moves = new ArrayList<>(this.moves);
        moves.add(move);

        return new Path(this.start, end, moves);
    }

    private Point _movePoint(Point p, Move m) {
        Point delta;
        switch (m) {
            case UP:
                delta = new Point(0, -1);
                break;
            case DOWN:
                delta = new Point(0, +1);
                break;
            case LEFT:
                delta = new Point(-1, 0);
                break;
            case RIGHT:
                delta = new Point(+1, 0);
                break;
            default:
                delta = new Point(0, 0);
        }

        return new Point((int) (p.getX() + delta.getX()), (int) (p.getY() + delta.getY()));
    }

    public String toString() {
        return String.format("%s, %s: %s", this.start, this.end, this.moves);
    }
}
