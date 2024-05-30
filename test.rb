RSpec.describe RubocopRules::CLI do
  describe '$ rubocop-rules' do
    it 'prints help text' do
      output = capture_stdout { RubocopRules::CLI::Commands.start([]) }
      expect(output).to include 'help [COMMAND]  # Describe available commands'
    end
  end
