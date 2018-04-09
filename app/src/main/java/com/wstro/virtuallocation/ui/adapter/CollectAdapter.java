package com.wstro.virtuallocation.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.wstro.virtuallocation.R;
import com.wstro.virtuallocation.data.model.RealData;

import java.util.List;

/**
 * 作者： ${PING} on 2018/4/9.
 */

public class CollectAdapter extends RecyclerView.Adapter {

    private List<RealData> datas;
    private Context mContext;

    public CollectAdapter(List<RealData> datas, Context mContext) {
        this.datas = datas;
        this.mContext = mContext;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CollectHolder(LayoutInflater.from(mContext).inflate(R.layout.item_collect,parent,false));
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        CollectHolder holder1 = (CollectHolder) holder;
        String addrStr = datas.get(position).getLocationInfo().getAddrStr();
        ((CollectHolder) holder).tvAddress.setText(addrStr == null ? "" : addrStr);
        holder1.getItemView().setClickable(true);
        holder1.getItemView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener!=null){
                    listener.click(datas.get(position));
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return datas.size();
    }



    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private OnItemClickListener listener;
    public interface OnItemClickListener{
        void click(RealData data);
    }


    public static  class CollectHolder extends RecyclerView.ViewHolder{

        private   View view;
        private TextView tvAddress;

        public CollectHolder(View itemView) {
            super(itemView);
            this.view=itemView;
              tvAddress = (TextView) itemView.findViewById(R.id.tv_address_collect);
        }

        public View getItemView() {
            return view;
        }
    }



}
