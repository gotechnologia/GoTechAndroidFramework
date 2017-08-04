package br.com.gotechnologia.framework.listener;

import android.view.View;

import br.com.gotechnologia.framework.media.MidiaUtil;

/**
 * Criado por Weslley Barbosa em 31/07/2017.
 */

public class ShowFileClickListener implements View.OnClickListener {

    private String path;

    public ShowFileClickListener(String path) {
        this.path = path;
    }

    @Override
    public void onClick(View v) {
         MidiaUtil.showFileAction(v.getContext(), path);
    }
}
