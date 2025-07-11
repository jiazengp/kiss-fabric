package com.kiss.data;

public final class PlayerSneakData {
    private int count;
    private int lastTick;
    private int particleLevel = 1;
    private boolean wasSneaking;

    public int count()           { return count; }
    public int lastTick()        { return lastTick; }
    public int particleLevel()   { return particleLevel; }
    public boolean wasSneaking() { return wasSneaking; }

    public void setCount(int count)                 { this.count = count; }
    public void resetCount()                        { count = 0; }
    public void setLastTick(int lastTick)           { this.lastTick = lastTick; }
    public void increaseParticleLevel(int max)      { if (particleLevel < max) particleLevel++; }
    public void decreaseParticleLevel(int step)     { particleLevel = Math.max(1, particleLevel - step); }
    public void setParticleLevel(int level)         { this.particleLevel = level; }
    public void setWasSneaking(boolean sneaking)    { this.wasSneaking = sneaking; }

    public void reset() {
        count = 0;
        lastTick = Integer.MIN_VALUE;
        particleLevel = 1;
        wasSneaking = false;
    }
}
