package com.gempukku.terasology.graphics.postprocess.bloom;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface BloomComponent extends Component {
    float getBlurRadius();
    void setBlurRadius(float blurRadius);

    float getMinimalBrightness();
    void setMinimalBrightness(float minimalBrightness);

    float getBloomStrength();

    void setBloomStrength(float bloomStrength);
}
