package com.gempukku.terasology.faction;

import com.gempukku.secsy.entity.Component;

import java.util.Set;

public interface FactionComponent extends Component {
    String getFactionId();

    Set<String> getOpposingFactions();
}
