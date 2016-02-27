package com.gempukku.secsy.entity.dispatch;

import com.gempukku.secsy.context.SystemContext;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.ContextAwareSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityEventListener;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.InternalEntityManager;
import com.gempukku.secsy.entity.event.Event;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@RegisterSystem
public class AnnotationDrivenEventDispatcher implements ContextAwareSystem<Object>, LifeCycleSystem, EntityEventListener {
    @In
    private InternalEntityManager internalEntityManager;

    private Multimap<Class<? extends Event>, EventListenerDefinition> eventListenerDefinitions = HashMultimap.create();
    private Iterable<Object> systems;

    @Override
    public void setContext(SystemContext<Object> context) {
        systems = context.getSystems();
    }

    @Override
    public void initialize() {
        internalEntityManager.addEntityEventListener(this);
    }

    @Override
    public void postInitialize() {
        for (Object system : systems) {
            scanSystem(system);
        }
    }

    private void scanSystem(Object system) {
        for (Method method : system.getClass().getDeclaredMethods()) {
            final ReceiveEvent receiveEventAnnotation = method.getAnnotation(ReceiveEvent.class);
            if (receiveEventAnnotation != null) {
                if (method.getReturnType().equals(Void.TYPE)
                        && Modifier.isPublic(method.getModifiers())) {
                    final Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length >= 2) {
                        if (Event.class.isAssignableFrom(parameters[0])
                                && EntityRef.class.isAssignableFrom(parameters[1])) {
                            boolean valid = true;
                            for (int i = 2; i < parameters.length; i++) {
                                if (!Component.class.isAssignableFrom(parameters[i])) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid) {
                                Class<? extends Component>[] components = new Class[parameters.length - 2];
                                for (int i = 2; i < parameters.length; i++) {
                                    components[i - 2] = (Class<? extends Component>) parameters[i];
                                }

                                eventListenerDefinitions.put((Class<? extends Event>) parameters[0],
                                        new EventListenerDefinition(system, method, components));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void eventSent(EntityRef entity, Event event) {
        for (EventListenerDefinition eventListenerDefinition : eventListenerDefinitions.get(event.getClass())) {
            boolean valid = true;
            for (Class<? extends Component> componentRequired : eventListenerDefinition.getComponentParameters()) {
                if (!entity.hasComponent(componentRequired)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                eventListenerDefinition.eventReceived(entity, event);
            }
        }
    }

    private class EventListenerDefinition {
        private Object system;
        private Method method;
        private Class<? extends Component>[] componentParameters;

        private EventListenerDefinition(Object system, Method method, Class<? extends Component>[] componentParameters) {
            this.system = system;
            this.method = method;
            this.componentParameters = componentParameters;
        }

        public Class<? extends Component>[] getComponentParameters() {
            return componentParameters;
        }

        public void eventReceived(EntityRef entity, Event event) {
            Object[] params = new Object[2 + componentParameters.length];
            params[0] = event;
            params[1] = entity;
            int index = 2;
            for (Class<? extends Component> componentParameter : componentParameters) {
                params[index++] = entity.getComponent(componentParameter);
            }

            try {
                method.invoke(system, params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
