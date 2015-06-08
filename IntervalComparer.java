package pl.edu.agh.pdptw;

import java.util.Comparator;

public class IntervalComparer implements Comparator<Interval>{

    @Override
    public int compare(Interval o1, Interval o2) {
        if (o1.begin < o2.begin) {
            return -1; 
        }
        else if(o2.begin < o1.begin)
        {
            return 1;
        }
        else if(o1.end < o2.end)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
