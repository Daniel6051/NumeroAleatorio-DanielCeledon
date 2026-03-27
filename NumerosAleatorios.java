//crear un proyecto en java y resolver el siguiente ejercicio:
//generar una serie de 500 números enteros aleatorios entre 10 y 1000
//calcular el promedio de estos números así como también la suma total de los mismos.
//Mostrar el resultado de estos valores obtenidos
//------..........................................--------------------------------....
//Nombre: Daniel Celedon 
//Legajo: 114422
import java.util.Random;

public class NumerosAleatorios {
  public static void main(String args[]) {
    Random NumeroAleatorio = new Random();
    int i=0,Acumulado=0;
    float promedio=0;
    for(i=0;i<500;i++){
        int a = 10 + NumeroAleatorio.nextInt(991);
        System.out.println("El numero aleatorio es: " + a);
        Acumulado=a+Acumulado;
    }
    promedio=(float) Acumulado/500;
    System.out.println("La Suma total es: " + Acumulado);
    System.out.println("El promedio es: " + promedio);
  }
}
