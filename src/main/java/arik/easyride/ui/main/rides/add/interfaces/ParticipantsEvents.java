package arik.easyride.ui.main.rides.add.interfaces;

import arik.easyride.models.User;

public interface ParticipantsEvents {

    void onAdd(User participant);

    void onRemove(User participant);
}
