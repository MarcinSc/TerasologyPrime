package com.gempukku.terasology.time.delay;

import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.ClassSystemProducer;
import com.gempukku.secsy.context.system.ShareSystemInitializer;
import com.gempukku.secsy.context.system.SimpleContext;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.time.TimeManager;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DelayedActionSystemTest {
    private DelayedActionSystem delayedActionSystem;
    private MockTimeManager mockTimeManager;
    private MockGameLoop mockGameLoop;

    @Before
    public void setup() {
        ClassSystemProducer classSystemProducer = new ClassSystemProducer();
        classSystemProducer.addClass(DelayedActionSystem.class);
        classSystemProducer.addClass(MockTimeManager.class);
        classSystemProducer.addClass(MockGameLoop.class);

        SimpleContext<Object> simpleContext = new SimpleContext<>();
        simpleContext.setSystemProducer(classSystemProducer);
        simpleContext.setSystemInitializer(new ShareSystemInitializer<>());

        simpleContext.startup();

        delayedActionSystem = (DelayedActionSystem) simpleContext.getSystem(DelayManager.class);
        mockTimeManager = (MockTimeManager) simpleContext.getSystem(TimeManager.class);
        mockGameLoop = (MockGameLoop) simpleContext.getSystem(GameLoop.class);

        delayedActionSystem.initialize();
    }

    @Test
    public void noEvents() {
        mockGameLoop.callUpdate();
    }

    @Test
    public void scheduleForFuture() {
        EntityRef entity = Mockito.mock(EntityRef.class);
        MockDelayedActionComponent dac = new MockDelayedActionComponent();
        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(null);
        Mockito.when(entity.createComponent(DelayedActionComponent.class)).thenReturn(dac);
        Mockito.when(entity.exists()).thenReturn(true);
        delayedActionSystem.addDelayedAction(entity, "actionId", 1000);

        Mockito.verify(entity).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).createComponent(DelayedActionComponent.class);
        Mockito.verify(entity).saveChanges();
        Mockito.verifyNoMoreInteractions(entity);

        assertEquals(1, dac.getActionIdWakeUp().size());
        assertEquals(1000, (long) dac.getActionIdWakeUp().get("actionId"));

        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(dac);

        mockTimeManager.setMultiverseTime(500);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(999);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(1000);
        mockGameLoop.callUpdate();

        Mockito.verify(entity).exists();
        Mockito.verify(entity, Mockito.times(2)).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).removeComponents(DelayedActionComponent.class);
        Mockito.verify(entity, Mockito.times(2)).saveChanges();
        Mockito.verify(entity).send(Matchers.argThat(
                new BaseMatcher<Event>() {
                    @Override
                    public boolean matches(Object o) {
                        return o instanceof DelayedActionTriggeredEvent && ((DelayedActionTriggeredEvent) o).actionId.equals("actionId");
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Incorrect event");
                    }
                }
        ));
        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(2000);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);
    }

    @Test
    public void rescheduleForward() {
        EntityRef entity = Mockito.mock(EntityRef.class);
        MockDelayedActionComponent dac = new MockDelayedActionComponent();
        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(null);
        Mockito.when(entity.createComponent(DelayedActionComponent.class)).thenReturn(dac);
        Mockito.when(entity.exists()).thenReturn(true);
        delayedActionSystem.addDelayedAction(entity, "actionId", 1000);

        Mockito.verify(entity).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).createComponent(DelayedActionComponent.class);
        Mockito.verify(entity).saveChanges();
        Mockito.verifyNoMoreInteractions(entity);

        assertEquals(1, dac.getActionIdWakeUp().size());
        assertEquals(1000, (long) dac.getActionIdWakeUp().get("actionId"));

        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(dac);

        delayedActionSystem.addDelayedAction(entity, "actionId", 2000);
        Mockito.verify(entity, Mockito.times(2)).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity, Mockito.times(2)).saveChanges();
        Mockito.verifyNoMoreInteractions(entity);

        assertEquals(1, dac.getActionIdWakeUp().size());
        assertEquals(2000, (long) dac.getActionIdWakeUp().get("actionId"));

        mockTimeManager.setMultiverseTime(500);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(1500);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(2000);
        mockGameLoop.callUpdate();

        Mockito.verify(entity).exists();
        Mockito.verify(entity, Mockito.times(3)).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).removeComponents(DelayedActionComponent.class);
        Mockito.verify(entity, Mockito.times(3)).saveChanges();
        Mockito.verify(entity).send(Matchers.argThat(
                new BaseMatcher<Event>() {
                    @Override
                    public boolean matches(Object o) {
                        return o instanceof DelayedActionTriggeredEvent && ((DelayedActionTriggeredEvent) o).actionId.equals("actionId");
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Incorrect event");
                    }
                }
        ));
        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(3000);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

    }

    @Test
    public void rescheduleBackward() {
        EntityRef entity = Mockito.mock(EntityRef.class);
        MockDelayedActionComponent dac = new MockDelayedActionComponent();
        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(null);
        Mockito.when(entity.createComponent(DelayedActionComponent.class)).thenReturn(dac);
        Mockito.when(entity.exists()).thenReturn(true);
        delayedActionSystem.addDelayedAction(entity, "actionId", 1000);

        Mockito.verify(entity).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).createComponent(DelayedActionComponent.class);
        Mockito.verify(entity).saveChanges();
        Mockito.verifyNoMoreInteractions(entity);

        assertEquals(1, dac.getActionIdWakeUp().size());
        assertEquals(1000, (long) dac.getActionIdWakeUp().get("actionId"));

        Mockito.when(entity.getComponent(DelayedActionComponent.class)).thenReturn(dac);

        delayedActionSystem.addDelayedAction(entity, "actionId", 800);
        Mockito.verify(entity, Mockito.times(2)).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity, Mockito.times(2)).saveChanges();
        Mockito.verifyNoMoreInteractions(entity);

        assertEquals(1, dac.getActionIdWakeUp().size());
        assertEquals(800, (long) dac.getActionIdWakeUp().get("actionId"));

        mockTimeManager.setMultiverseTime(500);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(1000);
        mockGameLoop.callUpdate();

        Mockito.verify(entity).exists();
        Mockito.verify(entity, Mockito.times(3)).getComponent(DelayedActionComponent.class);
        Mockito.verify(entity).removeComponents(DelayedActionComponent.class);
        Mockito.verify(entity, Mockito.times(3)).saveChanges();
        Mockito.verify(entity).send(Matchers.argThat(
                new BaseMatcher<Event>() {
                    @Override
                    public boolean matches(Object o) {
                        return o instanceof DelayedActionTriggeredEvent && ((DelayedActionTriggeredEvent) o).actionId.equals("actionId");
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Incorrect event");
                    }
                }
        ));
        Mockito.verifyNoMoreInteractions(entity);

        mockTimeManager.setMultiverseTime(2000);
        mockGameLoop.callUpdate();

        Mockito.verifyNoMoreInteractions(entity);
    }

    @RegisterSystem(
            shared = GameLoop.class)
    public static class MockGameLoop implements GameLoop {
        private GameLoopListener gameLoopListener;

        @Override
        public void addGameLoopListener(GameLoopListener gameLoopListener) {
            this.gameLoopListener = gameLoopListener;
        }

        @Override
        public void removeGameLoopListener(GameLoopListener gameLoopListener) {
            gameLoopListener = null;
        }

        public void callUpdate() {
            gameLoopListener.update();
        }
    }

    @RegisterSystem(
            shared = TimeManager.class)
    public static class MockTimeManager implements TimeManager {
        private long multiverseTime;

        public void setMultiverseTime(long multiverseTime) {
            this.multiverseTime = multiverseTime;
        }

        @Override
        public long getMultiverseTime() {
            return multiverseTime;
        }

        @Override
        public long getTimeSinceLastUpdate() {
            return 0;
        }

        @Override
        public float getWorldDayTime(String worldId) {
            return 0;
        }
    }

    private static class MockDelayedActionComponent implements DelayedActionComponent {
        private Map<String, Long> actionIdWakeUp;

        @Override
        public Map<String, Long> getActionIdWakeUp() {
            return actionIdWakeUp;
        }

        @Override
        public void setActionIdWakeUp(Map<String, Long> actionIdWakeUp) {
            this.actionIdWakeUp = actionIdWakeUp;
        }
    }
}