package test;

import java.awt.Point;

public abstract class Piece {
    final int DOWN = 0;
    final int LEFT = 1;
    final int RIGHT = 2;
    protected int r[];
    protected int c[];
    protected TetrisData data;
    protected Point center;
    public Piece(TetrisData data){
        r = new int[4];
        c = new int[4];
        this.data = data;
        center = new Point(5,0);

    } 
    public abstract int getType();
    public abstract int roteType();

    public int getX() { return center.x;}
    public int getY() { return center.y;}

    public boolean copy(){
        boolean value = false;
        int x = getX();
        int y = getY();
        if(getMinY() + y <= 0) {
            value = true;

        }
        for(int i=0; i < 4; i++){
            data.setAt(y + r[i], x + c[i], getType());
        }
        return value;

    }
    public boolean isOverlap(int dir){
        int x =getX();
        int y = getY();
        switch(dir) {
        case 0:
            for(int i=0; i < r.length; i++) {
                if(data.getAt(y+r[i]+1, x+c[i]) != 0){
                    return true;
                }
            }
            break;
        case 1:
            for(int i=0; i < r.length; i++) {
                if(data.getAt(y+r[i], x+c[i] -1) != 0) {
                    return true;
                }
            }
            break;
        case 2:
            for(int)
        }
    }

    
}
