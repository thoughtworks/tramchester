package com.tramchester.domain.id;

import com.tramchester.domain.places.PostcodeLocation;

public interface HasCaseInsensitiveId {
    CaseInsensitiveId<PostcodeLocation> getId();
}
