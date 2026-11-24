package com.productscout.starter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
public class MainActivity extends Activity {
    private final int ink=0xff18332e, muted=0xff52655f, green=0xff167957, bg=0xfff3f7f4;
    private LinearLayout root;
    private EditText barcode, cost, sale, fee, shipping;
    private TextView product, profit, history;
    private int lookupSequence=0;
    private int dp(float n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(-1,-2);a.topMargin=dp(top);p.addView(v,a);}
    private LinearLayout panel(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(shape(Color.WHITE,18));return l;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(15);b.setTextColor(primary?Color.WHITE:ink);b.setBackground(shape(primary?green:0xffe4efe8,12));b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(16);e.setTextColor(ink);e.setHintTextColor(muted);e.setHint(hint);e.setInputType(type);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(shape(bg,10));return e;}
    private void labelField(LinearLayout p,String label,EditText e){add(p,text(label,13,muted,true),12);add(p,e,5);}
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(24));scroll.addView(root);setContentView(scroll);
        add(root,text("MY PRODUCT SCOUT",12,green,true),0);add(root,text("Scan before you buy.",30,ink,true),8);add(root,text("Product information and resale estimates in one place.",15,muted,false),6);
        LinearLayout scan=panel();add(root,scan,18);add(scan,text("01  /  Identify a product",18,ink,true),0);
        barcode=field("UPC / EAN barcode",InputType.TYPE_CLASS_NUMBER);add(scan,barcode,12);add(scan,button("Scan barcode with camera",true,this::startScan),10);add(scan,button("Look up barcode",false,this::lookup),8);
        add(scan,text("Barcode searches send the code to Open Food Facts; camera frames are processed on the device.",12,muted,false),10);
        LinearLayout foodPanel=panel();add(root,foodPanel,12);add(foodPanel,text("02  /  Food information",18,ink,true),0);product=text("Scan or type a food barcode to see available label information.",15,ink,false);product.setTextIsSelectable(true);add(foodPanel,product,10);add(foodPanel,text("Community data can be incomplete or outdated. Always check the actual label, especially for allergies. No proprietary safety score is calculated.",12,muted,false),12);
        LinearLayout resalePanel=panel();add(root,resalePanel,12);add(resalePanel,text("03  /  Resale scout · CAD",18,ink,true),0);add(resalePanel,button("Search eBay Canada sold items",false,()->searchMarket("https://www.ebay.ca/sch/i.html?_nkw=","&LH_Complete=1&LH_Sold=1")),10);add(resalePanel,button("Search Amazon Canada",false,()->searchMarket("https://www.amazon.ca/s?k=","")),8);
        add(resalePanel,text("Enter your own verified selling price and fees. No live price or sales-rank feed is connected.",13,muted,false),12);
        int decimal=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL;
        cost=field("0.00",decimal);sale=field("0.00",decimal);fee=field("0.00",decimal);shipping=field("0.00",decimal);
        labelField(resalePanel,"Purchase cost (include tax)",cost);labelField(resalePanel,"Expected selling price",sale);labelField(resalePanel,"Marketplace fees (total CAD)",fee);labelField(resalePanel,"Shipping and other costs",shipping);add(resalePanel,button("Calculate estimated profit",true,this::calculate),14);profit=text("Enter values to calculate net profit and return on cost.",16,ink,true);add(resalePanel,profit,12);
        LinearLayout h=panel();add(root,h,12);add(h,text("Recent barcodes · on this phone",18,ink,true),0);history=text("",14,muted,false);history.setTextIsSelectable(true);add(h,history,10);add(h,button("Clear history",false,()->{getPreferences(MODE_PRIVATE).edit().remove("history").apply();refreshHistory();}),10);refreshHistory();
    }
    private void startScan(){new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES).setPrompt("Align the product barcode in the frame").setBeepEnabled(false).setOrientationLocked(false).initiateScan();}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){IntentResult result=IntentIntegrator.parseActivityResult(requestCode,resultCode,data);if(result!=null){if(result.getContents()!=null){barcode.setText(result.getContents());lookup();}}else super.onActivityResult(requestCode,resultCode,data);}
    private String code(){return barcode.getText().toString().trim();}
    private void searchMarket(String prefix,String suffix){String c=code();if(c.isEmpty()){Toast.makeText(this,"Scan or enter a barcode first.",Toast.LENGTH_SHORT).show();return;}try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(prefix+Uri.encode(c)+suffix)));}catch(Exception e){Toast.makeText(this,"No browser is available.",Toast.LENGTH_SHORT).show();}}
    private void remember(String code){try{JSONArray old=new JSONArray(getPreferences(MODE_PRIVATE).getString("history","[]"));JSONArray list=new JSONArray();list.put(code);for(int i=0;i<old.length()&&list.length()<12;i++){String v=old.optString(i);if(!code.equals(v))list.put(v);}getPreferences(MODE_PRIVATE).edit().putString("history",list.toString()).apply();refreshHistory();}catch(Exception ignored){}}
    private void refreshHistory(){try{JSONArray a=new JSONArray(getPreferences(MODE_PRIVATE).getString("history","[]"));StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){if(i>0)b.append("\n");b.append(a.getString(i));}history.setText(a.length()==0?"No scans yet.":b.toString());}catch(Exception e){history.setText("No scans yet.");}}
    private void lookup(){String c=code();if(!c.matches("[0-9]{8,14}")){product.setText("Enter a numeric barcode of 8–14 digits.");return;}remember(c);int request=++lookupSequence;product.setText("Looking up "+c+"…");new Thread(()->{String output;HttpURLConnection conn=null;try{URL url=new URL("https://world.openfoodfacts.org/api/v2/product/"+c+".json?fields=product_name,brands,ingredients_text,allergens,allergens_tags,nutriscore_grade,quantity");conn=(HttpURLConnection)url.openConnection();conn.setConnectTimeout(12000);conn.setReadTimeout(15000);conn.setRequestProperty("User-Agent","MyProductScoutStarter/1.0 (personal Android product lookup)");int status=conn.getResponseCode();if(status==404){output="No matching food record. Try the marketplace links below.";}else if(status!=200){output="Lookup unavailable (HTTP "+status+"). Try again later.";}else{try(InputStream in=conn.getInputStream();ByteArrayOutputStream buf=new ByteArrayOutputStream()){byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1)buf.write(block,0,n);JSONObject json=new JSONObject(buf.toString("UTF-8"));JSONObject p=json.optJSONObject("product");if(json.optInt("status")!=1||p==null){output="No matching food record. Try the marketplace links below.";}else{String grade=p.optString("nutriscore_grade","");if(!grade.matches("(?i)[a-e]"))grade="Not available";else grade=grade.toUpperCase(Locale.ROOT);output=value(p,"product_name","Unnamed product")+"\nBrand: "+value(p,"brands","Not available")+"\nSize: "+value(p,"quantity","Not available")+"\n\nNutri-Score: "+grade+"\n\nIngredients:\n"+value(p,"ingredients_text","Not available")+"\n\nReported allergens:\n"+value(p,"allergens","Not available — not evidence of absence")+"\n\nSource: Open Food Facts";}}}}catch(Exception e){output="Could not retrieve product information. Check your internet connection and try again.";}finally{if(conn!=null)conn.disconnect();}final String result=output;runOnUiThread(()->{if(request==lookupSequence&&!isFinishing())product.setText(result);});}).start();}
    private String value(JSONObject p,String key,String fallback){String s=p.optString(key,"").trim();return s.isEmpty()||"null".equals(s)?fallback:s;}
    private double number(EditText e,boolean required){String s=e.getText().toString().trim().replace(',','.');if(s.isEmpty()&&!required)return 0;double n=Double.parseDouble(s);if(!Double.isFinite(n)||n<0)throw new NumberFormatException();return n;}
    private void calculate(){try{double c=number(cost,true),s=number(sale,true),f=number(fee,false),sh=number(shipping,false);double net=s-c-f-sh;double total=c+f+sh;profit.setText(String.format(Locale.CANADA,"Estimated net profit: $%,.2f CAD\nROI on total cost: %s\nProfit margin: %s",net,total>0?String.format(Locale.CANADA,"%.1f%%",net/total*100):"N/A",s>0?String.format(Locale.CANADA,"%.1f%%",net/s*100):"N/A"));profit.setTextColor(net>0?green:net<0?0xffa23535:ink);}catch(Exception e){profit.setTextColor(0xffa23535);profit.setText("Enter valid non-negative amounts for purchase and selling price. Fees and shipping can be left blank.");}}
}
