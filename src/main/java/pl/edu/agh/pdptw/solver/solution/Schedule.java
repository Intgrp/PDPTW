package pl.edu.agh.pdptw.solver.solution;

import com.rits.cloning.Cloner;
import pl.edu.agh.pdptw.solver.configuration.*;
import pl.edu.agh.pdptw.solver.solution.interval.Interval;
import pl.edu.agh.pdptw.solver.solution.interval.IntervalComparer;
import pl.edu.agh.pdptw.solver.solution.interval.IntervalType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Schedule {
    
    private static int index = 1;
    
    private int id;
    private int holonCapacity;
    private int timeLimit;
    private Location locationBase;
    private List<Interval> intervals = new LinkedList<>();
    private Map<Integer, Commission> commissions = new HashMap<Integer, Commission>();
    private List<Integer> commissionIds = new LinkedList<>();
    
    public Schedule(int holonCapacity, int timeLimit, Location base)
    {
        this.id = index++;
        this.holonCapacity = holonCapacity;
        this.locationBase = base;
        this.timeLimit = timeLimit;
        Interval newInterval = new Interval(timeLimit, timeLimit, timeLimit, IntervalType.GOING_BACK_TO_BASE, 0, 0, this.locationBase);
        this.intervals.add(newInterval);
        this.reCountGoings();
    }
    
    public int getIndex()
    {
        return this.id;
    }
    
    public InsertProperties getBestInsertion(Commission commission)
    {
        double costBefore = countCost();
        
        InsertProperties bestScheduleProperties = null;
        Collections.sort(this.intervals, new IntervalComparer());
        Interval prev = null;
        
        Interval next;
        
        int listLength = intervals.size();
        int nowElementIndex = 1;
        
        while(nowElementIndex <= listLength)
        {
            Iterator<Interval> iterator = this.intervals.iterator();
            int i = 0;
            next = null;
            while(i < nowElementIndex)
            {
                prev = next;
                next = iterator.next();
                i++;
            }
            nowElementIndex ++;
            
            double insertIndex = checkIfInsertPossible(prev, next, commission);
            
            if(insertIndex > 0)
            {
                Interval prev2 = null;
                Interval next2 = null;
                int listLength2 = intervals.size();
                int nowElementIndex2 = 1;
                while(nowElementIndex2 <= listLength2)
                {
                    Iterator<Interval> iterator2 = this.intervals.iterator();
                    int j = 0;
                    next2 = null;
                    while(j < nowElementIndex2)
                    {
                        prev2 = next2;
                        next2 = iterator2.next();
                        j++;
                    }
                    nowElementIndex2 ++;
                 
                    if (prev2 != null && prev2.goingEnd >= insertIndex) {
                        double deliveryIndex = checkIfDeliveryPossible(prev2, next2, commission);
                        if (deliveryIndex >= 0){
                            reCountGoings();
                            if (checkIfCapacityOK()) {
                                double costNow = countCost();
                                double diffrenceCost = costNow - costBefore;
                                if (bestScheduleProperties == null || bestScheduleProperties.cost > diffrenceCost) {
                                    bestScheduleProperties = new InsertProperties();
                                    bestScheduleProperties.cost = diffrenceCost;
                                    bestScheduleProperties.pickupIndex = insertIndex;
                                    bestScheduleProperties.deliveryIndex = deliveryIndex;
                                    bestScheduleProperties.scheduleIndex = this.id;
                                }
                            }
                            removeDelivery(commission.getId());
                        }
                    }
                }
                removePickup(commission.getId());
            }    
            sortIntervals();
            reCountGoings();
        }
        return bestScheduleProperties;
    }
    
    private void reCountGoings()
    {
        Location prev = this.locationBase;
       
        sortIntervals();
        
        for(Interval x : intervals)
        {
            x.begin = x.goingEnd - prev.getDistanceTo(x.location);
            prev = x.location;
        }
    }

    public void sortIntervals() {
        Collections.sort(this.intervals, new IntervalComparer());
    }
    
    private double checkIfInsertPossible(Interval prev, Interval next, Commission commission) {
        double leftBorder = prev == null ? 0 : prev.end;
        Location prevLocation = prev == null ? this.locationBase : prev.location;
        
        double rightBorder = next.goingEnd;
        Location nextLocation = next.location;
        
        Location pickupLocation = commission.getPickupLocation();
        
        double timegoing1 = prevLocation.getDistanceTo(pickupLocation);
        double timegoing2 = pickupLocation.getDistanceTo(nextLocation);
        double serviceTime = commission.getPickupServiceTime();
        
        leftBorder += timegoing1;
        rightBorder -= timegoing2 + serviceTime;
        
        TimeWindow timeWindow = commission.getPickupTimeWindow();
        
        if (leftBorder <= rightBorder) {
            double indexToInsert = getIndexToInsertForPickup(leftBorder, rightBorder, timeWindow.begin, timeWindow.end);
            if (indexToInsert >= 0) 
            {
                Interval intervalToInsert = new Interval(indexToInsert, indexToInsert, indexToInsert + serviceTime, IntervalType.PICKUP, commission.getId(), commission.getPickupDemand(), commission.getPickupLocation());
                this.intervals.add(intervalToInsert);
                sortIntervals();
                return indexToInsert;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }
    }

    public double countCost() {
        double ret = 0;
        
        for(Interval x : intervals)
        {
            ret += x.goingEnd - x.begin;
        }
        
        return ret;     
    }

    private double getIndexToInsertForPickup(double leftBorder, double rightBorder, double begin, double end) {
        double left = Math.max(leftBorder, begin);
        double right = Math.min(rightBorder, end);
        if (left <= right) 
        {
            return right;
        }
        else
        {
            return -1;
        }
    }

    private double checkIfDeliveryPossible(Interval prev2, Interval next2, Commission commission) {
        double leftBorder = prev2 == null ? 0 : prev2.end;
        Location prevLocation = prev2 == null ? this.locationBase : prev2.location;
        
        double rightBorder = next2.goingEnd;
        Location nextLocation = next2.location;
        
        Location deliveryLocation = commission.getDeliveryLocation();
        
        double timegoing1 = prevLocation.getDistanceTo(deliveryLocation);
        double timegoing2 = deliveryLocation.getDistanceTo(nextLocation);
        double serviceTime = commission.getDeliveryServiceTime();
        
        leftBorder += timegoing1;
        rightBorder -= timegoing2 + serviceTime;
        
        TimeWindow timeWindow = commission.getDeliveryTimeWindow();
        
        if (leftBorder <= rightBorder) {
            double indexToInsert = getIndexToInsertForDelivery(leftBorder, rightBorder, timeWindow.begin, timeWindow.end);
            if (indexToInsert >= 0) 
            {
                Interval intervalToInsert = new Interval(indexToInsert, indexToInsert, indexToInsert + serviceTime, IntervalType.DELIVERY, commission.getId(), commission.getDeliveryDemand(), commission.getDeliveryLocation());
                this.intervals.add(intervalToInsert);
                sortIntervals();
                return indexToInsert;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }
    }

    private double getIndexToInsertForDelivery(double leftBorder, double rightBorder, double begin, double end) {
        double left = Math.max(leftBorder, begin);
        double right = Math.min(rightBorder, end);
        if (left <= right) 
        {
            return left;
        }
        else
        {
            return -1;
        }
    }

    private void removePickup(int id) {
        Iterator<Interval> iterator = this.intervals.iterator();
        while(iterator.hasNext())
        {
            Interval interval = iterator.next();
            if(interval.type == IntervalType.PICKUP && interval.commissionIndex == id)
            {
                iterator.remove();
                return;
            }
        }
        
        throw new IllegalArgumentException("Nie ma takeigo Id - pickup");
    }

    private void removeDelivery(int id) {
        Iterator<Interval> iterator = this.intervals.iterator();
        while(iterator.hasNext())
        {
            Interval interval = iterator.next();
            if(interval.type == IntervalType.DELIVERY && interval.commissionIndex == id)
            {
                iterator.remove();
                return;
            }
        }
        
        throw new IllegalArgumentException("Nie ma takeigo Id - delivery");
    }

    private boolean checkIfCapacityOK() {
        int capacity = 0;
        for(Interval x : this.intervals)
        {
            capacity += x.demand;
            if (capacity > this.holonCapacity)
            {
                return false;
            }
        }
        return true;
    }

    public void addCommissionPrecisively(InsertProperties properties, Commission commission) {
        Interval insertInterval = new Interval(properties.pickupIndex, properties.pickupIndex, properties.pickupIndex + commission.getPickupServiceTime(), IntervalType.PICKUP, commission.getId(), commission.getPickupDemand(), commission.getPickupLocation());
        Interval deliveryInterval = new Interval(properties.deliveryIndex, properties.deliveryIndex, properties.deliveryIndex + commission.getDeliveryServiceTime(), IntervalType.DELIVERY, commission.getId(), commission.getDeliveryDemand(), commission.getDeliveryLocation());
        this.intervals.add(insertInterval);
        this.intervals.add(deliveryInterval);
        sortIntervals();
        reCountGoings();
        this.commissions.put(commission.getId(), commission);
        this.commissionIds.add(commission.getId());
    }

    public void printIntervals() {
        sortIntervals();
        for(Interval x : this.intervals) {
            System.out.format("%5s%20s%20s%20s\n", x.commissionIndex, x.begin, x.end, x.type);
        }
    }
    
    public void removeCommission(int commissionId)
    {
        removeDelivery(commissionId);
        removePickup(commissionId);
        sortIntervals();
        reCountGoings();
        this.commissionIds.remove((Integer) commissionId);
        this.commissions.remove((Integer) commissionId);
    }

    public CommissionViewAndCost tryToAddBruteForce(Commission commission) {
        List<Interval> oldIntervals = this.intervals;
        double oldCost = countCost();
        CommissionViewAndCost best = null;
        List<int[]> permutations = generatePermutations(this.commissionIds, commission.getId());
        this.commissions.put(commission.getId(), commission);
        
        for (int[] x : permutations) 
        {
            CommissionViewAndCost result = checkPermutation(oldCost, x);
            if (result != null) 
            {
                if (best != null) 
                {
                    if (result.cost < best.cost) 
                    {
                        best = result;
                    }
                }
                else
                {
                    best = result;
                }
            }
        }
        
        this.intervals = oldIntervals;
        this.commissions.remove(commission.getId());
        
        return best;
        
    }

    private List<int[]> generatePermutations(List<Integer> commissionIds, int id) {
        Cloner cloner=new Cloner();
        List<int[]> permutations = new LinkedList<>();
        int[] initialArray = new int[commissionIds.size() + 1];
        int i = 0;
        for (int x : commissionIds) {
            initialArray[i++] = x;
        }
        initialArray[i++] = id;
        
        perm(commissionIds.size(), initialArray, permutations);
        
        return permutations;
    }
    
    private void perm(int k, int[] array, List<int[]> permutations)
    {
        if (k == 0) 
        {
            Cloner cloner=new Cloner();
            int[] arrayToAdd = cloner.deepClone(array);
            permutations.add(arrayToAdd);
        }
        else
        {
            for (int i = 0; i <= k; i++) {
               swap(array, i, k);
               perm(k-1, array, permutations);
               swap(array, i, k);
            }
        }
    }
    
    public void swap(int[] array, int a, int b)
    {
        int k = array[a];
        array[a] = array[b];
        array[b] = k;
    }

    private void resetSchedule() {
        this.intervals = new LinkedList<>();
        Interval newInterval = new Interval(timeLimit, timeLimit, timeLimit, IntervalType.GOING_BACK_TO_BASE, 0, 0, this.locationBase);
        this.intervals.add(newInterval);
        this.reCountGoings();
    }

    private CommissionViewAndCost checkPermutation(double oldCost, int[] x) {
        resetSchedule();
        
        for(int commissionId : x)
        {
            Commission commissionToInsert = this.commissions.get(commissionId);
            InsertProperties insertProperties = getBestInsertion(commissionToInsert);
            if (insertProperties == null) 
            {
                return null;
            }
            else
            {
                addCommissionPrecisively(insertProperties, commissionToInsert);
                this.commissionIds.remove((Integer)commissionToInsert.getId());
            }
        }
        
        CommissionViewAndCost ret = new CommissionViewAndCost();
        ret.cost = countCost() - oldCost;
        ret.intervals = this.intervals;
        ret.scheduleId = this.id;
        
        return ret;
    }

    void addCommissionPrecisively(CommissionViewAndCost best, Commission commission) 
    {
        this.intervals = best.intervals;
        this.commissions.put(commission.getId(), commission);
        this.commissionIds.add(commission.getId());
    }
    
    public List<Commission> getCommissions()
    {
        return new LinkedList<Commission>(this.commissions.values());
    }

    public List<Interval> getIntervals() {
        return this.intervals;
    }
}
