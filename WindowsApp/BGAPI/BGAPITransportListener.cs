namespace BGAPI
{
    public interface IBGAPITransportListener
    {
        void PacketSent(BGAPIPacket packet);
        void PacketReceived(BGAPIPacket packet);
    }
}