
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;
        Set<UTXO> seenUTXOs = new HashSet<>();

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (!utxoPool.contains(utxo)) {
                return false;
            }

            Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            if (seenUTXOs.contains(utxo)) {
                return false;
            }
            seenUTXOs.add(utxo);

            inputSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }
            outputSum += output.value;
        }

        return inputSum >= outputSum;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);

                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }

                byte[] txHash = tx.getHash();
                int index = 0;
                for (Transaction.Output output : tx.getOutputs()) {
                    UTXO utxo = new UTXO(txHash, index);
                    utxoPool.addUTXO(utxo, output);
                    index++;
                }
            }
        }

        return validTxs.toArray(new Transaction[validTxs.size()]);
    }
}
