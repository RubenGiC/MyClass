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

    public ArrayList<Asignatura> listNow;

    private static OnItemListener onItemListener;

    public ListaHorarioNow(ArrayList<Asignatura> listNow){//String[] dataSet
        this.listNow = listNow;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //diseño del reciclerview
        View vi = LayoutInflater.from(parent.getContext()).inflate(R.layout.subject_row, null, false);

        vi.setOnClickListener(new RV_AsigListener());

        return new ViewHolder(vi);
        //return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        /*
        holder.nombre.setText(listNow.get(position).getNombre)
        holder.nombre.setText(listNow.get(position).getAula)
        holder.nombre.setText(listNow.get(position).getHorario)
         */
        holder.hora.setText(listNow.get(position).getHora());
        holder.nombre.setText(listNow.get(position).getNombre());
        holder.aula.setText(listNow.get(position).getAula());
    }

    @Override
    public int getItemCount() {
        return listNow.size();
    }

    public Asignatura getAsignatura(int position){ return listNow.get(position);}

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView nombre, aula, hora;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            // Define the texts Views
            hora = (TextView) view.findViewById(R.id.Hora);
            nombre = (TextView) view.findViewById(R.id.Nombre);
            aula = (TextView) view.findViewById(R.id.Aula);
        }

        public TextView getName() {
            return nombre;
        }
    }

    //implement the interface callback method to transfer the click event to the external caller
    public static class RV_AsigListener implements View.OnClickListener{//, View.OnLongClickListener

        @Override
        public void onClick(View v) {
            if(onItemListener != null)
                onItemListener.OnItemClickListener(v, v.getId());

        }
    }

    public void setOnItemListener(OnItemListener listener){this.onItemListener = listener;}

    //define listening interface class
    public interface OnItemListener{
        void OnItemClickListener(View view, int position);
    }
}
