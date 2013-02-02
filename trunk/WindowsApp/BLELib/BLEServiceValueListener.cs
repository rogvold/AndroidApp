
namespace BLELib
{
    public interface BLEServiceValueListener
    {
        void receivedValue(BLEServiceInstance srv, byte[] value);
        void receivedInterval(BLEServiceInstance srv, int value);
    }
}
