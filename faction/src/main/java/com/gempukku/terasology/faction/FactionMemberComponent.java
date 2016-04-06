package com.gempukku.terasology.faction;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface FactionMemberComponent extends Component {
    String getFactionId();
}
