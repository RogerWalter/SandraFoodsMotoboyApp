package com.stizsoftware.sandrafoodsmotoboyapp.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stizsoftware.sandrafoodsmotoboyapp.R;
import com.stizsoftware.sandrafoodsmotoboyapp.model.Andamento;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.List;

import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_STATIC_DP;

public class AdapterMotoboyEntregas extends RecyclerView.Adapter<AdapterMotoboyEntregas.MyViewHolder>{
    private List<Andamento> listaPedidos;
    private int parametro = 0; //usado para ver qual tela está sendo exibida e qual dialogo abrir
    DatabaseReference firebaseBanco = FirebaseDatabase.getInstance().getReference();
    DatabaseReference andamentoDB;
    public AdapterMotoboyEntregas(List<Andamento> lista, int parametroRecebido) {
        this.listaPedidos = lista;
        this.parametro = parametroRecebido;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLista = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_lista_pedidos_motoboy, parent, false);

        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Andamento item = listaPedidos.get(position);

        holder.cliente.setText(item.getCliente());
        holder.valor.setText(item.getValor());
        holder.endereco.setText(item.getEndereco());

        holder.btWhats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String celular = item.getId().substring(0,11);
                String celularInicio = celular.substring(0,2);
                String celularFim = celular.substring(3,11);
                String celularFinal = "55" + celularInicio + celularFim;

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext())
                        .setTitle("Abrir WhatsApp")
                        .setMessage("O aplicativo deseja abrir o WhatsApp. Deseja permitir?")
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Uri uri = Uri.parse("smsto:" + celularFinal);
                                    Intent i = new Intent(Intent.ACTION_SENDTO, uri);
                                    //i.putExtra(Intent.EXTRA_TEXT, "Olá, gostaria de fazer um pedido!");
                                    i.setPackage("com.whatsapp");
                                    v.getContext().startActivity(i);
                                }catch(Exception e)
                                {
                                    try{
                                        Uri uri = Uri.parse("smsto:" + celularFinal);
                                        Intent j = new Intent(Intent.ACTION_SENDTO, uri);
                                        //j.putExtra(Intent.EXTRA_TEXT, "Olá, gostaria de fazer um pedido!");
                                        j.setPackage("com.whatsapp.w4b");
                                        v.getContext().startActivity(j);
                                    }catch (Exception f)
                                    {
                                        Toast.makeText(v.getContext(), "Ocorreu um erro ao abrir o aplicativo: " + f.toString(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                builder.show();
            }
        });

        holder.btMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enderecoPesquisar = item.getEnderecoPesquisa().replace(" ", "+");
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + enderecoPesquisar);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                v.getContext().startActivity(mapIntent);
            }
        });

        holder.btLigar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String celularLigar = item.getId().substring(0,11);
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + celularLigar));
                v.getContext().startActivity(intent);
            }
        });

        holder.btItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext());
                bottomSheetDialog.setContentView(R.layout.dialogo_motoboy_tela_principal);
                bottomSheetDialog.setCanceledOnTouchOutside(false);

                TextView tvStatus = bottomSheetDialog.findViewById(R.id.tvDialogoMotoboy2);
                TextView tvDescOrientacao = bottomSheetDialog.findViewById(R.id.tvDialogoMotoboy3);
                Button btVoltar = bottomSheetDialog.findViewById(R.id.btDialogoMotoboyVoltar);
                Button btConfirmar = bottomSheetDialog.findViewById(R.id.btDialogoMotoboyConfirmar);

                if(parametro == 0)
                {
                    tvStatus.setText("Saiu para Entrega");
                    tvDescOrientacao.setText("Verifique se a sua localização está ativada antes de sair do restaurante.");
                }
                else
                {
                    tvStatus.setText("Pedido Entregue");
                    tvDescOrientacao.setText("Se ocorreu algum problema ao fazer esta entrega, não faça nada e informe ao operador do restaurante");
                }

                PushDownAnim.setPushDownAnimTo(btVoltar)
                        .setScale( MODE_STATIC_DP, 8  )
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                bottomSheetDialog.dismiss();
                            }
                        });

                PushDownAnim.setPushDownAnimTo(btConfirmar)
                        .setScale( MODE_STATIC_DP, 8  )
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if(parametro == 0)
                                {
                                    String chavePedido = item.getId();
                                    andamentoDB = firebaseBanco.child("andamento");
                                    andamentoDB.child(chavePedido).child("status").setValue("SAIU");
                                    listaPedidos.remove(position);
                                }
                                else
                                {
                                    String chavePedido = item.getId();
                                    andamentoDB = firebaseBanco.child("andamento");
                                    andamentoDB.child(chavePedido).child("status").setValue("FINALIZADO");
                                    listaPedidos.remove(position);
                                }
                                bottomSheetDialog.dismiss();
                            }
                        });
                bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                bottomSheetDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder
    {
        TextView id;
        TextView status;
        TextView data;
        TextView cliente;
        TextView valor;
        TextView endereco;

        ImageButton btWhats, btMaps, btLigar;

        ConstraintLayout btItem;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            cliente = itemView.findViewById(R.id.tvAdapterMotoboyPedidosCliente);
            valor = itemView.findViewById(R.id.tvAdapterMotoboyPedidosValor);
            endereco = itemView.findViewById(R.id.tvAdapterMotoboyPedidosEndereco);
            btWhats = itemView.findViewById(R.id.tvAdapterMotoboyBtWhats);
            btLigar = itemView.findViewById(R.id.tvAdapterMotoboyBtLigar);
            btMaps = itemView.findViewById(R.id.tvAdapterMotoboyBtMaps);
            btItem =itemView.findViewById(R.id.itemListaMotoboyClicavel);
        }
    }

    public void listaComFiltro(ArrayList<Andamento> listaFiltrada)
    {
        listaPedidos = listaFiltrada;
        notifyDataSetChanged();
    }
}
