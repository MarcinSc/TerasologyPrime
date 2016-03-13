package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.network.serialize.EntityInformation;
import com.gempukku.secsy.network.serialize.EntitySerializationUtil;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class StreamClientCommunication implements ClientCommunication {
    private ObjectMapper objectMapper = new ObjectMapper();

    private InternalComponentManager componentManager;
    private EntityRef clientEntity;
    private OutputStream outputStream;

    private final List<Event> eventsToConsume = new LinkedList<>();

    public StreamClientCommunication(InternalComponentManager componentManager, EntityRef clientEntity,
                                     OutputStream outputStream) {
        this.componentManager = componentManager;
        this.clientEntity = clientEntity;
        this.outputStream = outputStream;
    }

    @Override
    public void addEntity(int entityId, EntityRef entity, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) throws IOException {
        outputStream.write(0);

        EntityInformation entityInformation = EntitySerializationUtil.serializeEntity(componentManager, clientEntity, entityId, entity, componentFieldFilters);
        objectMapper.writeValue(outputStream, entityInformation);
    }

    @Override
    public void updateEntity(int entityId, EntityRef entity, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) throws IOException {
        outputStream.write(1);

        EntityInformation entityInformation = EntitySerializationUtil.serializeEntity(componentManager, clientEntity, entityId, entity, componentFieldFilters);
        objectMapper.writeValue(outputStream, entityInformation);
    }

    @Override
    public void removeEntity(int entityId) throws IOException {
        outputStream.write(2);
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(entityId);
        dataOutputStream.flush();
    }

    @Override
    public void sendEventToClient(int entityId, Event event) throws IOException {
        outputStream.write(3);
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(entityId);
        dataOutputStream.writeUTF(event.getClass().getName());
        dataOutputStream.flush();
        objectMapper.writeValue(outputStream, event);
    }

    @Override
    public void commitChanges() throws IOException {
        outputStream.write(4);
    }

    @Override
    public void visitQueuedEvents(ServerEventVisitor visitor) {
        synchronized (eventsToConsume) {
            for (Event event : eventsToConsume) {
                visitor.visitEventReceived(event);
            }
        }
    }

    public void readEvents(InputStream inputStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        while (true) {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            String className = dataInputStream.readUTF();
            try {
                Event event = (Event) objectMapper.readValue(inputStream, Class.forName(className));
                synchronized (eventsToConsume) {
                    eventsToConsume.add(event);
                }
            } catch (ClassNotFoundException exp) {
                throw new IOException("Unable to create class object: " + className, exp);
            }
        }
    }
}
