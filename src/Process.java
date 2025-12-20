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

    // --- Added for AG Scheduler ---
    private int currentQuantumUsed = 0;

    public Process(int arriveTime, int burstTime, String name) {
        this.arrivalTime = arriveTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.name = name;
        waitingTime = 0;
    }

    // --- Added functions for AG Scheduler ---
    public void incrementUsed() { this.currentQuantumUsed++; }
    public void resetUsed() { this.currentQuantumUsed = 0; }
    public int getCurrentQuantumUsed() { return currentQuantumUsed; }
    public boolean isFinished() { return remainingTime == 0; }
    public void reduceRemaining() { this.remainingTime--; }

    // --- Original functions ---
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
}