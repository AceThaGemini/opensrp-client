package org.ei.opensrp.test.vaksinator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ei.opensrp.commonregistry.CommonPersonObject;
import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.commonregistry.CommonPersonObjectController;
import org.ei.opensrp.provider.SmartRegisterClientsProvider;
import org.ei.opensrp.service.AlertService;
import org.ei.opensrp.test.R;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.dialog.FilterOption;
import org.ei.opensrp.view.dialog.ServiceModeOption;
import org.ei.opensrp.view.dialog.SortOption;
import org.ei.opensrp.view.viewHolder.OnClickFormLauncher;

import java.text.SimpleDateFormat;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by user on 2/12/15.
 */

public class VaksinatorSmartClientsProvider implements SmartRegisterClientsProvider {

    private final LayoutInflater inflater;
    private final Context context;
    private final View.OnClickListener onClickListener;

    private final int txtColorBlack;
    private final AbsListView.LayoutParams clientViewLayoutParams;
    private Drawable iconPencilDrawable;
    protected CommonPersonObjectController controller;

    AlertService alertService;

    public VaksinatorSmartClientsProvider(Context context,
                                          View.OnClickListener onClickListener,
                                          CommonPersonObjectController controller, AlertService alertService) {
        this.onClickListener = onClickListener;
        this.controller = controller;
        this.context = context;
        this.alertService = alertService;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        clientViewLayoutParams = new AbsListView.LayoutParams(MATCH_PARENT,
                (int) context.getResources().getDimension(org.ei.opensrp.R.dimen.list_item_height));
        txtColorBlack = context.getResources().getColor(org.ei.opensrp.R.color.text_black);

    }

    private boolean checkViewHolderInstance(View convertView){
        if (convertView == null)
            return true;
        return (convertView.getTag() == null || !(convertView.getTag() instanceof  ViewHolder));
    }

    @Override
    public View getView(SmartRegisterClient smartRegisterClient, View convertView, ViewGroup viewGroup) {

        ViewHolder viewHolder;
        CommonPersonObjectClient pc = (CommonPersonObjectClient) smartRegisterClient;
        System.out.println(pc.getDetails().toString());
        if (viewGroup.getTag() == null || !(viewGroup.getTag() instanceof  ViewHolder)){
            convertView = (ViewGroup) inflater().inflate(R.layout.smart_register_jurim_client, null);
            viewHolder = new ViewHolder();
            viewHolder.profilelayout =  (LinearLayout)convertView.findViewById(R.id.profile_info_layout);

//----------Child Basic Information
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.motherName = (TextView)convertView.findViewById(R.id.motherName);
            viewHolder.village = (TextView)convertView.findViewById(R.id.village);
            viewHolder.age = (TextView)convertView.findViewById(R.id.age);
            viewHolder.gender = (TextView)convertView.findViewById(R.id.gender);

//----------FrameLayout
            viewHolder.hb0Layout = (FrameLayout)convertView.findViewById(R.id.hb0Layout);
            viewHolder.bcgLayout = (FrameLayout)convertView.findViewById(R.id.bcgLayout);
            viewHolder.hb1Layout = (FrameLayout)convertView.findViewById(R.id.hb1Layout);
            viewHolder.hb2Layout = (FrameLayout)convertView.findViewById(R.id.hb2Layout);
            viewHolder.hb3Layout = (FrameLayout)convertView.findViewById(R.id.hb3Layout);
            viewHolder.campakLayout = (FrameLayout)convertView.findViewById(R.id.campakLayout);

//----------TextView to show immunization date
            viewHolder.hb0 = (TextView)convertView.findViewById(R.id.hb1);
            viewHolder.pol1 = (TextView)convertView.findViewById(R.id.pol1);
            viewHolder.pol2 = (TextView)convertView.findViewById(R.id.pol2);
            viewHolder.pol3 = (TextView)convertView.findViewById(R.id.pol3);
            viewHolder.pol4 = (TextView)convertView.findViewById(R.id.pol4);
            viewHolder.campak = (TextView)convertView.findViewById(R.id.ipv);

//----------Checklist logo
            viewHolder.hbLogo = (ImageView)convertView.findViewById(R.id.hbLogo);
            viewHolder.pol1Logo = (ImageView)convertView.findViewById(R.id.pol1Logo);
            viewHolder.pol2Logo = (ImageView)convertView.findViewById(R.id.pol2Logo);
            viewHolder.pol3Logo = (ImageView)convertView.findViewById(R.id.pol3Logo);
            viewHolder.pol4Logo = (ImageView)convertView.findViewById(R.id.pol4Logo);
            viewHolder.ipvLogo = (ImageView)convertView.findViewById(R.id.measlesLogo);

            viewHolder.profilepic =(ImageView)convertView.findViewById(R.id.profilepic);
            viewHolder.follow_up = (ImageButton)convertView.findViewById(R.id.btn_edit);
            viewHolder.profilepic.setImageDrawable(context.getResources().getDrawable(R.mipmap.household_profile_thumb));

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) viewGroup.getTag();
        }

        viewHolder.follow_up.setOnClickListener(onClickListener);
        viewHolder.follow_up.setTag(smartRegisterClient);
        viewHolder.profilelayout.setOnClickListener(onClickListener);
        viewHolder.profilelayout.setTag(smartRegisterClient);

        if (iconPencilDrawable == null) {
            iconPencilDrawable = context.getResources().getDrawable(R.drawable.ic_pencil);
        }
        viewHolder.follow_up.setImageDrawable(iconPencilDrawable);
        viewHolder.follow_up.setOnClickListener(onClickListener);

        int umur = pc.getDetails().get("tanggal_lahir") != null ? age(pc.getDetails().get("tanggal_lahir")) : 0;

        if (pc.getDetails().get("profilepic") != null) {
            VaksinatorDetailActivity.setImagetoHolderFromUri((Activity) context, pc.getDetails().get("profilepic"), viewHolder.profilepic, R.drawable.child_boy_infant);
            viewHolder.profilepic.setTag(smartRegisterClient);
        }
        else {
            viewHolder.profilepic.setImageDrawable(pc.getDetails().get("jenis_kelamin") != null
                            ? context.getResources().getDrawable(pc.getDetails().get("jenis_kelamin").contains("em")
                            ? R.drawable.child_girl_infant
                            : R.drawable.child_boy_infant)
                            : context.getResources().getDrawable(R.drawable.child_boy_infant)
            );
        }

        viewHolder.name.setText(pc.getDetails().get("nama_bayi") != null ? pc.getDetails().get("nama_bayi") : " ");

        viewHolder.motherName.setText(
            pc.getDetails().get("namaIbu")!=null
                ? pc.getDetails().get("namaIbu")
                : pc.getDetails().get("nama_orang_tua")!=null
                    ? pc.getDetails().get("nama_orang_tua")
                    :" ");

        viewHolder.village.setText(pc.getDetails().get("village")!= null
                ? pc.getDetails().get("village").length()>4
                    ? pc.getDetails().get("village")
                    : pc.getDetails().get("dusun")!= null
                        ? pc.getDetails().get("dusun")
                        : " "
                : pc.getDetails().get("dusun")!= null
                    ? pc.getDetails().get("dusun")
                    : " ");
        viewHolder.age.setText(pc.getDetails().get("tanggal_lahir")!=null
                ?     Integer.toString(age(pc.getDetails().get("tanggal_lahir"))/12)+" "+ context.getResources().getString(R.string.year_short)
                    + ", "+Integer.toString(age(pc.getDetails().get("tanggal_lahir"))%12)+" "+ context.getResources().getString(R.string.month_short)
                : " ");
        viewHolder.gender.setText(pc.getDetails().get("jenis_kelamin") != null
                ? pc.getDetails().get("jenis_kelamin").contains("em")
                    ? "Perempuan"
                    : "Laki-laki"
                : " ");

        viewHolder.hb0.setText(latestDate(new String[]{pc.getDetails().get("hb0")}));

        viewHolder.pol1.setText(
                latestDate(new String[]{pc.getDetails().get("bcg"),pc.getDetails().get("polio1")})
        );

        viewHolder.pol2.setText(
                latestDate(new String[]{pc.getDetails().get("dpt_hb1"),pc.getDetails().get("polio2")})
        );

        viewHolder.pol3.setText(
                latestDate(new String[]{pc.getDetails().get("dpt_hb2"),pc.getDetails().get("polio3")})
        );

        viewHolder.pol4.setText(
                latestDate(new String[]{pc.getDetails().get("dpt_hb3"),pc.getDetails().get("polio4")/*,pc.getDetails().get("ipv")*/})
        );

        viewHolder.campak.setText(pc.getDetails().get("imunisasi_campak") != null ? pc.getDetails().get("imunisasi_campak") : " ");

//----- logo visibility, sometimes the variable contains blank string that count as not null, so we must check both the availability and content
        boolean a = hasDate(pc,"hb0") || hasDate(pc,"hb1_kurang_7_hari") || hasDate(pc,"hb1_lebih_7_hari");
        viewHolder.hbLogo.setImageResource(a ? R.drawable.ic_yes_large : umur > 0 ? R.drawable.vacc_late : umur == 0 ? R.mipmap.vacc_due : R.drawable.abc_list_divider_mtrl_alpha);

        setIcon(viewHolder.bcgLayout,viewHolder.pol1Logo,"bcg,polio1",umur,1,pc);
        setIcon(viewHolder.hb1Layout,viewHolder.pol2Logo,"dpt_hb1,polio2",umur,2,pc);
        setIcon(viewHolder.hb2Layout,viewHolder.pol3Logo,"dpt_hb2,polio3",umur,3,pc);
        setIcon(viewHolder.hb3Layout,viewHolder.pol4Logo,"dpt_hb3,polio4",umur,4,pc);
        setIcon(viewHolder.campakLayout,viewHolder.ipvLogo,"imunisasi_campak",umur,9,pc);

        convertView.setLayoutParams(clientViewLayoutParams);
        return convertView;
    }

    private String latestDate(String[]dates){
        String max = dates[0]!=null
                     ? dates[0].length()==10
                        ? dates[0]
                        : "0000-00-00"
                     :"0000-00-00";
        for(int i=1;i<dates.length;i++){
            if(dates[i]==null)
                continue;
            if(dates[i].length()<10)
                continue;
            max =   (((Integer.parseInt(max.substring(0,4))-Integer.parseInt(dates[i].substring(0,4)))*360)
                    +((Integer.parseInt(max.substring(5,7))-Integer.parseInt(dates[i].substring(5,7)))*30)
                    +(Integer.parseInt(max.substring(8,10))-Integer.parseInt(dates[i].substring(8,10)))
                    )<0 ? dates[i]:max;
        }
        return max.equals("0000-00-00")? "" : max;
    }

    //  updating icon
    private void setIcon(FrameLayout frame,ImageView image,/* String oldCode,*/ String detailID,int umur, int indicator, CommonPersonObjectClient pc) {
        frame.setBackgroundColor(context.getResources().getColor(R.color.abc_background_cache_hint_selector_material_light));
        String[]var = detailID.split(",");
        boolean complete = false;
        boolean someComplete = false;

        for(int i=0;i<var.length;i++){
            someComplete = someComplete || (pc.getDetails().get(var[i]) != null && pc.getDetails().get(var[i]).length()>6);
        }

        if(someComplete){
            complete=true;
            for(int i=0;i<var.length;i++){
                complete = complete && (pc.getDetails().get(var[i]) != null && pc.getDetails().get(var[i]).length()>6);
            }
        }

        image.setImageResource(complete
            ? R.drawable.ic_yes_large
            : someComplete
                ? R.drawable.vacc_due
                : umur > indicator
                    ? R.drawable.vacc_late
                    : R.drawable.abc_list_divider_mtrl_alpha
        );

        if((umur == indicator) && !(complete || someComplete))
            frame.setBackgroundColor(context.getResources().getColor(R.color.vaccBlue));
    }

    private boolean hasDate(CommonPersonObjectClient pc, String variable){
        return pc.getDetails().get(variable)!=null && pc.getDetails().get(variable).length()>6;
    }

    //  month age calculation
    private int age(String date){
        String []dateOfBirth = date.split("-");
        String []currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()).split("-");

        int tahun = Integer.parseInt(currentDate[0]) - Integer.parseInt(dateOfBirth[0]);
        int bulan = Integer.parseInt(currentDate[1]) - Integer.parseInt(dateOfBirth[1]);
        int hari = Integer.parseInt(currentDate[2]) - Integer.parseInt(dateOfBirth[2]);

        int result = ((tahun*360) + (bulan*30) + hari)/30;
        result = result < 0 ? 0 : result;

        return result;
    }

    CommonPersonObjectController householdelcocontroller;

    @Override
    public SmartRegisterClients getClients() {
        return controller.getClients("form_ditutup","yes");
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption villageFilter, ServiceModeOption serviceModeOption,
                                              FilterOption searchFilter, SortOption sortOption) {
        return getClients().applyFilter(villageFilter, serviceModeOption, searchFilter, sortOption);
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {
        // do nothing.
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String formName, String entityId, String metaData) {
        return null;
    }

    public LayoutInflater inflater() {
        return inflater;
    }

    class ViewHolder {
         LinearLayout profilelayout;
         ImageView profilepic;
         ImageButton follow_up;
         public TextView hb0;
         public TextView pol1;
         public TextView pol2;
         public TextView complete;
         public TextView name;
         public TextView motherName;
         public TextView village;
         public TextView age;
         public TextView pol3;
         public TextView pol4;
         public TextView campak;
         public TextView gender;
         public ImageView hbLogo;
         public ImageView pol1Logo;
         public ImageView pol2Logo;
         public ImageView pol3Logo;
         public ImageView pol4Logo;
         public ImageView ipvLogo;
         public FrameLayout hb0Layout;
         public FrameLayout bcgLayout;
         public FrameLayout hb1Layout;
         public FrameLayout hb2Layout;
         public FrameLayout hb3Layout;
         public FrameLayout campakLayout;
     }
}

