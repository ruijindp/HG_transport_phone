package com.hgkefang.transport.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hgkefang.transport.R;
import com.hgkefang.transport.entity.RetData;

import java.util.List;

/**
 * Create by admin on 2018/9/7
 */
public class TestAdapter extends BaseAdapter {

    private List<RetData> results;

    public TestAdapter(List<RetData> results) {
        this.results = results;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Object getItem(int position) {
        return results.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_named_entity, parent, false);
            convertView.setTag(new ViewHolder(convertView));
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        RetData retData = results.get(position);
        holder.tv.setText(retData.getTradition_hotel_name());
        return convertView;
    }

    static class ViewHolder {
        TextView tv;

        ViewHolder(View view) {
            tv = view.findViewById(R.id.tvItem);
        }

    }

}
