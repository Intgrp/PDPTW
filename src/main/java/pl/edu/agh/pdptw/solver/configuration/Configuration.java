package pl.edu.agh.pdptw.solver.configuration;

import pl.edu.agh.pdptw.solver.configuration.Commission;
import pl.edu.agh.pdptw.solver.configuration.Location;

import java.util.Collections;
import java.util.List;

public class Configuration {
    private Location baseLocation;
    private List<Commission> commissions;
    private int timeLimit;
    private int holonCapacity;
    
    public Configuration(List<Commission> commissions, int timeLimit, int holonCapacity, Location baseLocation)
    {
        this.baseLocation = baseLocation;
        this.timeLimit = timeLimit;
        this.holonCapacity = holonCapacity;
        this.commissions = commissions;
    }
    
    public Location getBaseLocation()
    {
        return baseLocation;
    }
    
    public int getTimeLimit()
    {
        return timeLimit;
    }
    
    public int getCapacity()
    {
        return holonCapacity;
    }
    
    public List<Commission> getCommissions()
    {
        return commissions;
    }
    
    public void ShuffleCommissions()
    {
        Collections.shuffle(commissions);
    }
}
