namespace BLELib
{
    public interface BLEServiceValueListener
    {
        void ReceivedValue(BLEServiceInstance srv, byte[] value);
        void ReceivedInterval(BLEServiceInstance srv, int value);
    }
}