package com.gempukku.terasology.graphics.postprocess.blur;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface BlurComponent extends Component {
    float getBlurRadius();

    void setBlurRadius(float blurRadius);
}
