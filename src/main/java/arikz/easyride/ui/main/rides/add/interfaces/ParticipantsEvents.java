package arikz.easyride.ui.main.rides.add.interfaces;

import arikz.easyride.models.User;

public interface ParticipantsEvents {

    void onAdd(User participant);

    void onRemove(User participant);
}
