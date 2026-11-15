package dev.iconnavigator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Readable offline policy, with optional links opened by external apps. */
public final class PrivacyActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);getWindow().setDecorFitsSystemWindows(false);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(12),dp(20),dp(20));
        body.setBackgroundColor(0xff101310);
        TextView title=new TextView(this);title.setText("Privacy & support");title.setTextSize(26);title.setTextColor(0xffb9f97b);body.addView(title);
        TextView text=new TextView(this);text.setTextSize(16);text.setTextColor(0xffedf3e9);text.setPadding(0,dp(16),0,dp(16));
        StringBuilder policy=new StringBuilder();
        try(var reader=new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.privacy_policy),StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null)policy.append(line).append('\n');
        } catch(java.io.IOException e) {throw new IllegalStateException("Cannot read packaged privacy policy",e);}
        text.setText(policy);body.addView(text);
        add(body,"Read policy online","https://github.com/erikhardlyworking/icon-navigator/blob/main/PRIVACY.md");
        add(body,"Email Hardly Working","mailto:workinghardlyforyou@gmail.com");
        Button back=new Button(this);back.setText("Back to settings");back.setAllCaps(false);back.setOnClickListener(v->finish());body.addView(back);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.BLACK);scroll.addView(body);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{var bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        setContentView(scroll);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void add(LinearLayout body,String label,String uri){
        Button button=new Button(this);button.setText(label);button.setAllCaps(false);button.setMinHeight(dp(48));
        button.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(uri)));}
            catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"No app is available to open this link.",Toast.LENGTH_SHORT).show();}});
        body.addView(button);
    }
}
