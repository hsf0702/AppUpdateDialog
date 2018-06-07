package sskj.lee.appupdatedialog;

import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import sskj.lee.appupdatelibrary.BaseUpdateDialogFragment;
import sskj.lee.appupdatelibrary.BaseVersion;
import sskj.lee.appupdatelibrary.SimpleUpdateFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.dialog:{
                SimpleUpdateFragment updateFragment = new SimpleUpdateFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(BaseUpdateDialogFragment.INTENT_KEY, initData(BaseVersion.DEFAULT_STYLE));
                updateFragment.setArguments(bundle);
                FragmentManager transition = getFragmentManager();
                updateFragment.show(transition, "tag");
            }
                break;
            case R.id.notifycation:
                SimpleUpdateFragment updateFragment = new SimpleUpdateFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(BaseUpdateDialogFragment.INTENT_KEY, initData(BaseVersion.NOTIFYCATION_STYLE));
                updateFragment.setArguments(bundle);
                FragmentManager transition = getFragmentManager();
                updateFragment.show(transition, "tag");
                break;
        }
    }

    private VersionInfo initData(int dialogStyle) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setUrl("http://www.paicl.net/download/lela.apk");
        versionInfo.setViewStyle(dialogStyle);
        return versionInfo;
    }
}