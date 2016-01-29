package com.ucsf.ui;


import com.ucsf.services.ServerUploaderService;

/**
 * Patient phone implementation of the {@link com.ucsf.core_phone.ui.NewProfileActivity
 * new profile activity}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class NewProfileActivity extends com.ucsf.core_phone.ui.NewProfileActivity {
    @Override
    protected ServerUploaderService.Provider getServerUploaderServiceProvider() {
        return ServerUploaderService.getProvider(this);
    }
}
