import java.util.ArrayList;

public class Process {
    private int arrivalTime;
    private int burstTime;
    private int priority;
    private int remainingTime;
    private int waitingTime;
    private int turnAroundTime;
    private int quantum;
    private ArrayList<Integer> quantumHistory = new ArrayList<>();
    private String name;
    private int waitingCounter;

    // --- Added for AG Scheduler ---
    private int currentQuantumUsed = 0;

    public Process(int arriveTime, int burstTime, String name) {
        this.arrivalTime = arriveTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.name = name;
        waitingTime = 0;
        // The first quantum is usually added upon initialization in the AG logic
    }

    // --- Added functions for AG Scheduler ---
    public void incrementUsed() { this.currentQuantumUsed++; }
    public void resetUsed() { this.currentQuantumUsed = 0; }
    public int getCurrentQuantumUsed() { return currentQuantumUsed; }
    public boolean isFinished() { return remainingTime == 0; }
    public void reduceRemaining() { this.remainingTime--; }

    // --- NEW: Threshold helpers to match the screenshot logic ---
    
    /**
     * Calculates the non-preemptive limit (ceil of 50%).
     * As seen in the screenshot: P1 (Q=7) -> ceil(50%) = 4.
     */
    public int getNonPreemptiveLimit() {
        return (int) Math.ceil(0.5 * this.quantum);
    }

    /**
     * Calculates the priority check limit (ceil of 25%).
     * As seen in the screenshot: P1 (Q=7) -> ceil(25%) = 2.
     */
    public int getPriorityLimit() {
        return (int) Math.ceil(0.25 * this.quantum);
    }

    // --- Original functions (RETAINED) ---
    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }
    public void setBurstTime(int burstTime) { this.burstTime = burstTime; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getPriority() { return priority; }
    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }
    public void setTurnAroundTime(int turnAroundTime) { this.turnAroundTime = turnAroundTime; }
    public int getTurnAroundTime() { return turnAroundTime; }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        addQuantumHistory(quantum); // Log every update
    }
    public int getQuantum() { return quantum; }
    public void addQuantumHistory(int quantum){ this.quantumHistory.add(quantum); }
    public ArrayList<Integer> getQuantumHistory(){ return quantumHistory; }
    public String getName() { return name; }
    
    public void resetWaitingCounter() {
        this.waitingCounter = 0;
    }
    public void incrementWaitingCounter() {
        this.waitingCounter++;
    }
    public int getWaitingCounter() {
        return this.waitingCounter;
    } 

    public void updateQuantum(int quantum) {
        this.quantum = quantum;
        addQuantumHistory(quantum); // Only log when quantum actually changes
    }
}
