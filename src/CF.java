import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Luka on 06/06/2017.
 */
public class CF {
    private static int n;
    private static int m;
    private static double[][] itemSim;
    private static double[][] userSim;
    private static double[] itemAverages;
    private static double[] userAverages;
    private static double[][] userItemMat;

    private static class MyComparator implements Comparator<Double>{
        @Override
        public int compare(Double o1, Double o2) {
            return -o1.compareTo(o2);
        }
    }

    private static double simCosine(int i, int j, boolean forItems,boolean pearsonModification){
        double res = 0;
        double ri = 0, rj = 0;
        double k;

        //similarity for items
        if(forItems){
            if(pearsonModification){
                if(itemAverages == null){
                    itemAverages = new double[n];
                    for(int row=0; row<n; row++){
                        k = 0;
                        itemAverages[row] = 0;
                        for(int col=0; col<m; col++){
                            itemAverages[row] += userItemMat[row][col];
                            if(userItemMat[row][col] != 0){
                                k++;
                            }
                        }
                        itemAverages[row] /= k;
                    }
                }

                for(int col=0; col<m; col++){
                    ri += (userItemMat[i][col]==0 ? 0 : (userItemMat[i][col]-itemAverages[i])*(userItemMat[i][col]-itemAverages[i]));
                    rj += (userItemMat[j][col]==0 ? 0 : (userItemMat[j][col]-itemAverages[j])*(userItemMat[j][col]-itemAverages[j]));
                    res+= (userItemMat[i][col]==0 || userItemMat[j][col]==0 ? 0 : (userItemMat[i][col]-itemAverages[i])*(userItemMat[j][col]-itemAverages[j]));
                }
            }
            else{
                for(int col=0; col<m; col++){
                    ri += userItemMat[i][col]*userItemMat[i][col];
                    rj += userItemMat[j][col]*userItemMat[j][col];
                    res += userItemMat[i][col]*userItemMat[j][col];
                }
            }
        }
        //similarity for users
        else{
            if(pearsonModification){
                if( userAverages == null){
                    userAverages = new double[m];
                    for(int col=0; col<m; col++){
                        k = 0;
                        userAverages[col] = 0;
                        for(int row=0; row<n; row++){
                            userAverages[col] += userItemMat[row][col];
                            if(userItemMat[row][col] != 0){
                                k++;
                            }
                        }
                        userAverages[col] /= k;
                    }
                }

                for(int row=0; row<n; row++){
                    ri += (userItemMat[row][i]==0? 0:(userItemMat[row][i]-userAverages[i])*(userItemMat[row][i]-userAverages[i]));
                    rj += (userItemMat[row][j]==0? 0:(userItemMat[row][j]-userAverages[j])*(userItemMat[row][j]-userAverages[j]));
                    res += (userItemMat[row][i]==0 || userItemMat[row][j]==0 ? 0 : (userItemMat[row][i]-userAverages[i])*(userItemMat[row][j]-userAverages[j]));
                }
            }
            else{
                for(int row=0; row<n; row++){
                    ri += userItemMat[row][i]*userItemMat[row][i];
                    rj += userItemMat[row][j]*userItemMat[row][j];
                    res += userItemMat[row][i]*userItemMat[row][j];
                }
            }
        }

        ri = Math.sqrt(ri);
        rj = Math.sqrt(rj);
        res /= (ri*rj);
        return res;
    }

    private static double calculateQuery(int item, int user, int algType, int k){
        double sumOfSims = 0;
        double sumOfSimsRates = 0;
        //Item-item Collaborative filtering
        if(algType == 0){
            if(itemSim == null){
                itemSim = new double[n][n];
                for(int col=0; col<n-1; col++){             //calculating down-left triangle of matrix and copying to upper-right
                    for(int row=col+1; row<n; row++){
                        itemSim[row][col] = itemSim[col][row] = simCosine(row, col, true, true);
                    }
                }
            }

            TreeMap<Double, Integer> sims = new TreeMap<>(new MyComparator());
            for(int i=0; i<n; i++){
                if( i != item && userItemMat[i][user] != 0){
                    sims.put(itemSim[item][i], i);
                }
            }

            int lim = 0;
            for(Double key : sims.keySet()){
                if(++lim > k){
                    break;
                }
                sumOfSims += key;
                sumOfSimsRates += key*(userItemMat[sims.get(key)][user]);
            }
        }
        //User-user Collaborative filtering
        else{
            if(userSim == null){
                userSim = new double[m][m];
                for(int col=0; col<m-1; col++){             //calculating down-left triangle of matrix and copying to upper-right
                    for(int row=col+1; row<m; row++){
                        userSim[row][col] = userSim[col][row] = simCosine(row, col, false, true);
                    }
                }
            }

            TreeMap<Double, Integer> sims = new TreeMap<>(new MyComparator());
            for(int i=0; i<m; i++){
                if( i != user && userItemMat[item][i] != 0){
                    sims.put(userSim[user][i], i);
                }
            }

            int lim = 0;
            for(Double key : sims.keySet()){
                if(++lim > k || key <= 0){
                    break;
                }
                sumOfSims += key;
                sumOfSimsRates += key*(userItemMat[item][sims.get(key)]);
            }
        }

        return sumOfSimsRates/sumOfSims;
    }

    public static void main(String[] args) throws IOException {
        //byte[] bytes = Files.readAllBytes(Paths.get("tests/test1/R.in"));
        BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));//new ByteArrayInputStream(bytes)));//

        String[] lineArr =rd.readLine().split(" ");
        n = Integer.parseInt(lineArr[0]);
        m = Integer.parseInt(lineArr[1]);

        userItemMat = new double[n][m];
        for(int i=0; i<n; i++){
            lineArr = rd.readLine().split(" ");
            for(int j=0; j<m; j++){
                userItemMat[i][j] = lineArr[j].compareTo("X") == 0 ? 0 : Integer.parseInt(lineArr[j]);
            }
        }

        int q = Integer.parseInt(rd.readLine());
        double res;
        DecimalFormat df = new DecimalFormat("#.000");
        BigDecimal bd, result;
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<q; i++){
            lineArr = rd.readLine().split(" ");
            res = calculateQuery(Integer.parseInt(lineArr[0])-1,
                            Integer.parseInt(lineArr[1])-1,
                            Integer.parseInt(lineArr[2]),
                            Integer.parseInt(lineArr[3]));
            bd = new BigDecimal(res);
            result = bd.setScale(3, RoundingMode.HALF_UP);
            sb.append(result)
                .append("\n");
        }

        //String rec = new String(Files.readAllBytes(Paths.get("tests/test1/R.out")));
        //compareResults(sb.toString(), rec);
        System.out.print(sb.toString());
    }

    private static void compareResults(String calc, String recieved) {
        String[] calculatedLines = calc.split("\n");
        String[] recievedLines = recieved.split("\n");

        //System.out.println("Calculated lines: "+calculatedLines.length+"\nRecieved lines: "+recievedLines.length);
        for(int i=0; i<calculatedLines.length; i++){
            if(calculatedLines[i].compareTo(recievedLines[i]) != 0){
                System.out.println("line no."+i+" and diff is calc:"+calculatedLines[i]+" vs. rec:"+recievedLines[i]);
            }
        }

        System.out.print(calc.compareTo(recieved.toString())==0 ? "Izlazi su isti" : "Ima greski");
    }
}
