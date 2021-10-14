package org.rubengic.myclass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListaHorarioNow  extends RecyclerView.Adapter<ListaHorarioNow.ViewHolder> {

    public ArrayList<AsignaturaAula> listNow;

    private static OnItemListener onItemListener;

    public ListaHorarioNow(ArrayList<AsignaturaAula> listNow){//String[] dataSet
        this.listNow = listNow;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //diseño del reciclerview
        //View vi = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_row_item, null, false);

        //vi.setOnClickListener(new RV_ItemListener());
        //vi.setOnLongClickListener(new RV_ItemListener());

        //return new ViewHolder(vi);
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name, price, market;
        ImageView imagen;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            // Define the texts Views
            /*name = (TextView) view.findViewById(R.id.t_name);
            hora = (TextView) view.findViewById(R.id.t_hora);
            aula = (TextView) view.findViewById(R.id.t_aula);
            */
        }

        public TextView getName() {
            return name;
        }
    }

    //implement the interface callback method to transfer the click event to the external caller
    public static class RV_AsigListener implements View.OnClickListener{//, View.OnLongClickListener

        @Override
        public void onClick(View v) {
            if(onItemListener != null)
                onItemListener.OnItemClickListener(v, v.getId());

        }

        /*@Override
        public boolean onLongClick(View v) {
            if(onItemListener != null)
                onItemListener.OnItemLongClickListener(v, v.getId());
            return true;
        }*/
    }

    //define listening interface class
    public interface OnItemListener{
        void OnItemClickListener(View view, int position);
        //void OnItemLongClickListener(View view, int position);
    }
}
