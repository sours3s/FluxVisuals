package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.type.CountLoopStrategy;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DelayStep;
import ru.fluxvisuals.ui.gui.clickgui.util.type.LoopStrategy;
import ru.fluxvisuals.ui.gui.clickgui.util.type.PerformAction;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Stopwatch;
import ru.fluxvisuals.ui.gui.clickgui.util.model.TickStep;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ActionScheduler {
    public int currentStepIndex;
    public int currentTickStepIndex;
    public int currentTick;
    public boolean interrupt;
    public final Stopwatch time = new Stopwatch(false);
    public final List<DelayStep> scriptSteps = Lists.newCopyOnWriteArrayList();
    public final List<TickStep> scriptTickSteps = Lists.newCopyOnWriteArrayList();
    public LoopStrategy loopStrategy = new CountLoopStrategy(1);

    public ActionScheduler() {
        cleanup();
    }

    public ActionScheduler addStep(int i, PerformAction performAction) {
        return addStep(i, performAction, () -> {
            return true;
        }, 0);
    }

    public ActionScheduler addStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier) {
        return addStep(i, performAction, booleanSupplier, 0);
    }

    public ActionScheduler addStep(int i, PerformAction performAction, int i2) {
        return addStep(i, performAction, () -> {
            return true;
        }, i2);
    }

    public ActionScheduler addStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier, int i2) {
        this.scriptSteps.add(new DelayStep(i, performAction, booleanSupplier, i2));
        Collections.sort(this.scriptSteps);
        return this;
    }

    public ActionScheduler addTickStep(int i, PerformAction performAction) {
        return addTickStep(i, performAction, () -> {
            return true;
        }, 0);
    }

    public ActionScheduler addTickStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier) {
        return addTickStep(i, performAction, booleanSupplier, 0);
    }

    public ActionScheduler addTickStep(int i, PerformAction performAction, int i2) {
        return addTickStep(i, performAction, () -> {
            return true;
        }, i2);
    }

    public ActionScheduler addTickStep(int i, PerformAction performAction, BooleanSupplier booleanSupplier, int i2) {
        this.scriptTickSteps.add(new TickStep(i, performAction, booleanSupplier, i2));
        Collections.sort(this.scriptTickSteps);
        return this;
    }

    public void resetTime() {
        this.time.reset();
    }

    public void resetStepIndex() {
        this.currentStepIndex = 0;
        this.currentTickStepIndex = 0;
        this.currentTick = 0;
    }

    public ActionScheduler cleanupIfFinished() {
        if (isFinished()) {
            cleanup();
        }
        return this;
    }

    public ActionScheduler cleanup() {
        this.scriptSteps.clear();
        this.scriptTickSteps.clear();
        resetTime();
        resetStepIndex();
        return this;
    }

    public ActionScheduler update() {
        if ((this.scriptSteps.isEmpty() && this.scriptTickSteps.isEmpty()) || this.interrupt) {
            return this;
        }
        this.scriptSteps.forEach(step -> {
            if (this.currentStepIndex < this.scriptSteps.size()) {
                DelayStep delayStep = this.scriptSteps.get(this.currentStepIndex);
                if (delayStep.condition().getAsBoolean() && this.time.hasElapsed(delayStep.delay())) {
                    delayStep.action().perform();
                    this.currentStepIndex++;
                    resetTime();
                    if (this.loopStrategy.shouldLoop(this.currentStepIndex, this.scriptSteps.size())) {
                        resetStepIndex();
                        this.loopStrategy.onLoop();
                    }
                }
            }
        });
        if (this.currentTickStepIndex < this.scriptTickSteps.size()) {
            TickStep tickStep = this.scriptTickSteps.get(this.currentTickStepIndex);
            if (!tickStep.condition().getAsBoolean()) {
                this.currentTick++;
                return this;
            }
            if (tickStep.ticks() <= this.currentTick) {
                tickStep.action().perform();
                this.currentTickStepIndex++;
                if (this.loopStrategy.shouldLoop(this.currentTickStepIndex, this.scriptTickSteps.size())) {
                    resetStepIndex();
                    this.loopStrategy.onLoop();
                }
            }
        }
        this.currentTick++;
        this.currentStepIndex = Math.min(this.currentStepIndex, this.scriptSteps.size());
        this.currentTickStepIndex = Math.min(this.currentTickStepIndex, this.scriptTickSteps.size());
        return this;
    }

    public ActionScheduler setLoopStrategy(LoopStrategy loopStrategy269) {
        this.loopStrategy = loopStrategy269;
        return this;
    }

    public boolean isFinished() {
        return this.currentStepIndex >= this.scriptSteps.size() && this.currentTickStepIndex >= this.scriptTickSteps.size() && !this.interrupt && this.loopStrategy.isFinished();
    }

    public Stopwatch getTime() {
        return this.time;
    }

    public List<DelayStep> getScriptSteps() {
        return this.scriptSteps;
    }

    public List<TickStep> getScriptTickSteps() {
        return this.scriptTickSteps;
    }

    public int getCurrentStepIndex() {
        return this.currentStepIndex;
    }

    public int getCurrentTickStepIndex() {
        return this.currentTickStepIndex;
    }

    public int getCurrentTick() {
        return this.currentTick;
    }

    public boolean isInterrupt() {
        return this.interrupt;
    }

    public LoopStrategy getLoopStrategy() {
        return this.loopStrategy;
    }

    public void setCurrentStepIndex(int i) {
        this.currentStepIndex = i;
    }

    public void setCurrentTickStepIndex(int i) {
        this.currentTickStepIndex = i;
    }

    public void setCurrentTick(int i) {
        this.currentTick = i;
    }

    public void setInterrupt(boolean z) {
        this.interrupt = z;
    }
}
